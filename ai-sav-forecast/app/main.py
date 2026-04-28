"""
main.py
FastAPI — ai-sav-forecast :8003
3 endpoints :
  GET  /api/v1/health
  POST /api/v1/forecast
  GET  /api/v1/forecast/info
"""
import time
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.logging import get_logger
from app.schemas import (
    ForecastRequest, ForecastResponse,
    HealthResponse, ModelInfoResponse, ConfidenceLevel
)
from app.preprocessor import preprocess, get_warning
from app.forecaster import run_forecast
from app.db import get_stored_forecast
from app.schemas import StoredForecastResponse

logger = get_logger(__name__)


# ── Startup / Shutdown ───────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info(f"Starting {settings.SERVICE_NAME} v{settings.VERSION}")
    logger.info(f"ENV={settings.ENV} PORT={settings.PORT}")
    yield
    logger.info(f"Arrêt {settings.SERVICE_NAME}")


# ── App ──────────────────────────────────────────────────────────
app = FastAPI(
    title       = "AI-SAV Forecast",
    description = "Prédiction volumes tickets BCS — Prophet",
    version     = settings.VERSION,
    docs_url    = "/api/docs",
    lifespan    = lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins  = ["*"],
    allow_methods  = ["*"],
    allow_headers  = ["*"],
)


# ── GET /api/v1/health ───────────────────────────────────────────
@app.get(
    "/api/v1/health",
    response_model = HealthResponse,
    tags           = ["Health"]
)
async def health():
    """Vérifie que le service est opérationnel."""
    try:
        from prophet import Prophet
        prophet_ok = True
    except ImportError:
        prophet_ok = False

    return HealthResponse(
        status     = "ok",
        service    = settings.SERVICE_NAME,
        version    = settings.VERSION,
        prophet_ok = prophet_ok,
    )


# ── GET /api/v1/forecast/result ──────────────────────────────────
@app.get(
    "/api/v1/forecast/result",
    response_model = StoredForecastResponse,
    tags           = ["Forecast"]
)
async def get_forecast_result(agence: str):
    """
    Retourne les prédictions déjà calculées par ForecastJob (batch nuit).
    Lit directement depuis la table `forecasts` en DB — sans recalculer Prophet.
    
    Appelé par ai-sav-rag quand un utilisateur demande la prédiction d'une agence.
    Le batch tourne chaque nuit à 1h, donc les données sont toujours fraîches.
    """
    agence = agence.strip().upper()

    if agence not in settings.AGENCES_VALIDES:
        raise HTTPException(
            status_code = 404,
            detail      = f"Agence inconnue : {agence}. Valides : {settings.AGENCES_VALIDES}"
        )

    try:
        result = get_stored_forecast(agence)
    except Exception as e:
        logger.error(f"Erreur DB forecast result | agence={agence} | {e}")
        raise HTTPException(
            status_code = 503,
            detail      = f"Erreur accès base de données : {str(e)}"
        )

    if result is None:
        raise HTTPException(
            status_code = 404,
            detail      = (
                f"Aucune prédiction disponible pour {agence}. "
                f"Le batch ForecastJob n'a pas encore tourné ou aucune donnée n'existe."
            )
        )

    return StoredForecastResponse(**result)


# ── POST /api/v1/forecast ────────────────────────────────────────
@app.post(
    "/api/v1/forecast",
    response_model = ForecastResponse,
    tags           = ["Forecast"]
)
async def forecast(request: ForecastRequest):
    """
    Prédit les volumes de tickets pour les prochains jours.

    Appelé par ForecastJob (ai-sav-batch) chaque nuit à 1h.
    Retourne prédictions total + CRITIQUE + MAPE + confidence.
    """
    start = time.time()
    agence = request.agence

    logger.info(
        f"Requête forecast | agence={agence} | "
        f"historique={len(request.historique)} points | "
        f"horizon={request.horizon_jours}j"
    )

    # Validation minimum de points
    if len(request.historique) < 10:
        raise HTTPException(
            status_code = 422,
            detail      = f"Historique insuffisant : {len(request.historique)} points (minimum 10)"
        )

    try:
        # ── Preprocessing
        df_full, df_prophet_total, df_prophet_critique, granularity = preprocess(
            request.historique
        )

        # Validation après preprocessing
        if len(df_prophet_total) < 5:
            raise HTTPException(
                status_code = 422,
                detail      = f"Données insuffisantes après nettoyage : {len(df_prophet_total)} points valides"
            )

        # ── Forecast
        result = run_forecast(
            df_prophet_total    = df_prophet_total,
            df_prophet_critique = df_prophet_critique,
            granularity         = granularity,
            horizon             = request.horizon_jours,
            agence              = agence,
        )

        elapsed = round(time.time() - start, 2)
        logger.info(f"Forecast terminé | agence={agence} | durée={elapsed}s")

        return ForecastResponse(**result)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Erreur forecast | agence={agence} | {str(e)}")
        raise HTTPException(
            status_code = 500,
            detail      = f"Erreur interne : {str(e)}"
        )


# ── GET /api/v1/forecast/info ────────────────────────────────────
@app.get(
    "/api/v1/forecast/info",
    response_model = ModelInfoResponse,
    tags           = ["Forecast"]
)
async def forecast_info(agence: str):
    """
    Informations sur les données disponibles pour une agence.
    Utilisé par le dashboard pour afficher le niveau de confiance.
    """
    agence = agence.upper()

    if agence not in settings.AGENCES_VALIDES:
        raise HTTPException(
            status_code = 404,
            detail      = f"Agence inconnue : {agence}. Valides : {settings.AGENCES_VALIDES}"
        )

    # Info statique basée sur l'analyse des données
    info_par_agence = {
        "JARMOUN": {
            "n_points"       : 84,
            "date_min"       : "2025-01-12",
            "date_max"       : "2026-01-30",
            "moy_jour"       : 12.8,
            "confidence_level": ConfidenceLevel.HIGH,
            "granularity_rec": "daily",
            "warning"        : None,
        },
        "LAHDIY": {
            "n_points"       : 106,
            "date_min"       : "2025-06-25",
            "date_max"       : "2026-02-09",
            "moy_jour"       : 23.8,
            "confidence_level": ConfidenceLevel.MEDIUM,
            "granularity_rec": "daily",
            "warning"        : "Volatilité élevée (CV=119%) — prédictions indicatives",
        },
        "LAHRYA": {
            "n_points"       : 62,
            "date_min"       : "2025-07-14",
            "date_max"       : "2026-01-08",
            "moy_jour"       : 17.5,
            "confidence_level": ConfidenceLevel.LOW,
            "granularity_rec": "weekly",
            "warning"        : "Données insuffisantes pour prédiction journalière — estimation hebdomadaire",
        },
    }

    data = info_par_agence[agence]
    return ModelInfoResponse(agence=agence, **data)