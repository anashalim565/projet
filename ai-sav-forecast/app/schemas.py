"""
schemas.py
Contrat API — ce que ai-sav-batch envoie
et ce que ai-sav-forecast retourne.
"""
from pydantic import BaseModel, Field, field_validator
from typing import List, Optional
from enum import Enum

class Agence(str, Enum):
    JARMOUN = "JARMOUN"
    LAHDIY  = "LAHDIY"
    LAHRYA  = "LAHRYA"


class Granularity(str, Enum):
    DAILY  = "daily"
    WEEKLY = "weekly"


class ConfidenceLevel(str, Enum):
    HIGH   = "HIGH"    # MAPE < 20%
    MEDIUM = "MEDIUM"  # MAPE 20-35%
    LOW    = "LOW"     # MAPE > 35% ou données insuffisantes


# ── INPUT ────────────────────────────────────────────────────────

class TicketDataPoint(BaseModel):
    """Un point journalier envoyé par ForecastJob."""
    date    : str = Field(..., example="2025-07-14",
                          description="Format YYYY-MM-DD")
    total   : int = Field(..., ge=0,
                          description="Nombre total tickets ce jour")
    critique: int = Field(..., ge=0,
                          description="Nombre tickets CRITIQUE ce jour")

class HistoriquePoint(BaseModel):
    date: str
    total: int
    critique: int

class ForecastRequest(BaseModel):
    agence: str = Field(..., min_length=1)
    horizon_jours: int = Field(..., ge=1, le=365)
    historique: List[HistoriquePoint] = Field(..., min_length=1)

    @field_validator("agence")
    @classmethod
    def validate_agence(cls, v: str) -> str:
        v = v.strip().upper()
        if not v:
            raise ValueError("agence vide")
        return v


# ── OUTPUT ───────────────────────────────────────────────────────

class PredictionPoint(BaseModel):
    """Une prédiction pour un jour."""
    date       : str
    yhat       : float = Field(..., description="Prédiction centrale")
    yhat_lower : float = Field(..., description="Borne basse 80%")
    yhat_upper : float = Field(..., description="Borne haute 80%")
    is_weekend : bool  = False
    is_holiday : bool  = False


class ForecastResponse(BaseModel):
    """
    Réponse POST /api/v1/forecast
    ForecastJob sauvegarde ces données dans table forecasts.
    """
    agence              : str
    granularity         : str
    horizon_jours       : int
    predictions_total   : List[PredictionPoint]
    predictions_critique: List[PredictionPoint]
    pct_critique_prevu  : Optional[float] = Field(
        None,
        description="% moyen CRITIQUE prévu sur l'horizon"
    )
    mape_total          : Optional[float] = None
    mape_critique       : Optional[float] = None
    confidence          : ConfidenceLevel
    n_points_used       : int
    warning             : Optional[str]   = None

class StoredForecastResponse(BaseModel):
    """
    GET /api/v1/forecast/result — retourne les prédictions déjà calculées
    par le batch ForecastJob, lues depuis la table forecasts en DB.
    """
    agence               : str
    granularity          : str
    confidence           : str
    mape                 : Optional[float] = None
    pct_critique_prevu   : Optional[float] = None
    predictions_total    : List[PredictionPoint]
    predictions_critique : List[PredictionPoint]
    generated_at         : str
    warning              : Optional[str] = None


# ── HEALTH ───────────────────────────────────────────────────────

class HealthResponse(BaseModel):
    status     : str  = "ok"
    service    : str  = "ai-sav-forecast"
    version    : str  = "1.0.0"
    prophet_ok : bool = True


class ModelInfoResponse(BaseModel):
    """GET /api/v1/forecast/info — infos par agence."""
    agence           : str
    n_points         : int
    date_min         : str
    date_max         : str
    moy_jour         : float
    confidence_level : ConfidenceLevel
    granularity_rec  : str
    warning          : Optional[str] = None