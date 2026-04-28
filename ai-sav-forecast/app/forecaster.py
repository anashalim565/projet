"""
forecaster.py — fix : growth='flat' pour éviter l'extrapolation baissière.

Raison : avec minusYears(1), Prophet voit une baisse July→Jan et
extrapole ~0 pour les 30 prochains jours. 
growth='flat' désactive la tendance globale et se concentre
sur la saisonnalité hebdomadaire — plus pertinent pour la planification.
"""
import math
import warnings
from typing import List, Tuple, Optional

import numpy as np
import pandas as pd
from prophet import Prophet
from prophet.diagnostics import cross_validation, performance_metrics

warnings.filterwarnings("ignore")

from app.config import settings
from app.logging import get_logger
from app.preprocessor import MOROCCO_HOLIDAYS, get_warning
from app.schemas import PredictionPoint, ConfidenceLevel

logger = get_logger(__name__)


def build_prophet_model(granularity: str) -> Prophet:
    model = Prophet(
        growth                  = "flat",           # FIX : pas de tendance globale
        seasonality_mode        = settings.SEASONALITY_MODE,
        changepoint_prior_scale = settings.CHANGEPOINT_PRIOR,
        seasonality_prior_scale = settings.SEASONALITY_PRIOR,
        interval_width          = settings.INTERVAL_WIDTH,
        yearly_seasonality      = False,
        weekly_seasonality      = True if granularity == "daily" else False,
        daily_seasonality       = False,
        holidays                = MOROCCO_HOLIDAYS,
    )

    if granularity == "daily":
        model.add_seasonality(
            name          = "monthly",
            period        = 30.5,
            fourier_order = 3,
        )

    return model


def _calculate_mape(
    df_prophet: pd.DataFrame,
    horizon: int,
    granularity: str,
    target_name: str
) -> Optional[float]:
    n = len(df_prophet)

    if granularity == "weekly":
        min_points  = 10
        horizon_str = f"{math.ceil(horizon / 7)} W"
        initial_str = f"{max(4, math.ceil(n * 0.5))} W"
        period_str  = f"{max(2, math.ceil(horizon / 7))} W"
    else:
        min_points  = 30
        horizon_str = f"{horizon} days"
        initial_str = f"{max(14, int(n * 0.5))} days"
        period_str  = f"{max(7, horizon // 2)} days"

    if n < min_points:
        logger.warning(f"MAPE non calculé [{target_name}] — points insuffisants ({n} < {min_points})")
        return None

    try:
        model = build_prophet_model(granularity)
        model.fit(df_prophet)

        df_cv = cross_validation(
            model,
            initial      = initial_str,
            period       = period_str,
            horizon      = horizon_str,
            disable_tqdm = True,
        )

        if df_cv is None or df_cv.empty:
            logger.warning(f"MAPE non calculé [{target_name}] — cross_validation vide")
            return None

        df_perf = performance_metrics(df_cv, metrics=["mape"])

        if df_perf is None or df_perf.empty or "mape" not in df_perf.columns:
            logger.warning(f"MAPE non calculé [{target_name}] — performance_metrics vide")
            return None

        mape_values = df_perf["mape"].dropna()
        if mape_values.empty:
            return None

        mape = round(float(mape_values.mean()) * 100, 1)
        logger.info(f"MAPE [{target_name}] = {mape}%")
        return mape

    except Exception as e:
        logger.warning(f"MAPE calcul échoué [{target_name}] : {e}")
        return None


def predict(
    df_prophet: pd.DataFrame,
    granularity: str,
    horizon: int,
    target_name: str
) -> Tuple[pd.DataFrame, Optional[float]]:
    logger.info(
        f"Fit Prophet [{target_name}] | {len(df_prophet)} points | "
        f"granularity={granularity} | horizon_jours={horizon}"
    )

    model = build_prophet_model(granularity)
    model.fit(df_prophet)

    if granularity == "weekly":
        periods = math.ceil(horizon / 7)
        freq    = "W"
    else:
        periods = horizon
        freq    = "D"

    future   = model.make_future_dataframe(periods=periods, freq=freq)
    forecast = model.predict(future)

    forecast["yhat"]       = forecast["yhat"].clip(lower=0)
    forecast["yhat_lower"] = forecast["yhat_lower"].clip(lower=0)
    forecast["yhat_upper"] = forecast["yhat_upper"].clip(lower=0)

    mape = _calculate_mape(df_prophet, horizon, granularity, target_name)

    logger.info(f"Prédiction terminée [{target_name}] | periods={periods} | freq={freq} | MAPE={mape}")
    return forecast, mape


def build_prediction_points(
    forecast   : pd.DataFrame,
    horizon    : int,
    granularity: str,
    last_date  : pd.Timestamp   # ← ajouter ce paramètre
) -> List[PredictionPoint]:
    
    # Prendre UNIQUEMENT les dates APRÈS la dernière date des données
    future_forecast = forecast[forecast["ds"] > last_date].copy()

    # Limiter au nombre de points voulus
    n = math.ceil(horizon / 7) if granularity == "weekly" else horizon
    future_forecast = future_forecast.head(n)

    points = []
    for _, row in future_forecast.iterrows():
        points.append(PredictionPoint(
            date       = row["ds"].strftime("%Y-%m-%d"),
            yhat       = round(float(row["yhat"]), 1),
            yhat_lower = round(float(row["yhat_lower"]), 1),
            yhat_upper = round(float(row["yhat_upper"]), 1),
            is_weekend = row["ds"].dayofweek >= 5,
            is_holiday = bool(pd.Timestamp(row["ds"]) in MOROCCO_HOLIDAYS["ds"].values),
        ))
    return points


def get_confidence_level(mape, n_points, granularity):
    if granularity == "weekly" and n_points < settings.MIN_POINTS_DAILY:
        return ConfidenceLevel.LOW
    if mape is None:
        return ConfidenceLevel.MEDIUM
    if mape < settings.MAPE_HIGH_THRESHOLD:
        return ConfidenceLevel.HIGH
    elif mape < settings.MAPE_MEDIUM_THRESHOLD:
        return ConfidenceLevel.MEDIUM
    return ConfidenceLevel.LOW


def calculate_pct_critique(predictions_total, predictions_critique):
    total_sum    = sum(p.yhat for p in predictions_total    if not p.is_weekend)
    critique_sum = sum(p.yhat for p in predictions_critique if not p.is_weekend)
    if total_sum == 0:
        return None
    return round(critique_sum / total_sum * 100, 1)


def run_forecast(df_prophet_total, df_prophet_critique, granularity, horizon, agence):
    logger.info(f"Début forecast | agence={agence} | horizon={horizon}j | granularity={granularity}")

    forecast_total,    mape_total    = predict(df_prophet_total,    granularity, horizon, "total")
    forecast_critique, mape_critique = predict(df_prophet_critique, granularity, horizon, "critique")

    last_date = df_prophet_total["ds"].max()
    last_date_critique = df_prophet_critique["ds"].max()


    predictions_total    = build_prediction_points(forecast_total,    horizon, granularity, last_date)
    predictions_critique = build_prediction_points(forecast_critique, horizon, granularity, last_date_critique)

    pct_critique = calculate_pct_critique(predictions_total, predictions_critique)
    n_points     = len(df_prophet_total)
    confidence   = get_confidence_level(mape_total, n_points, granularity)
    warning      = get_warning(n_points, granularity, mape_total)

    logger.info(
        f"Forecast terminé | agence={agence} | "
        f"confidence={confidence} | mape_total={mape_total} | pct_critique={pct_critique}%"
    )

    return {
        "agence"              : agence,
        "granularity"         : granularity,
        "horizon_jours"       : horizon,
        "predictions_total"   : predictions_total,
        "predictions_critique": predictions_critique,
        "pct_critique_prevu"  : pct_critique,
        "mape_total"          : mape_total,
        "mape_critique"       : mape_critique,
        "confidence"          : confidence,
        "n_points_used"       : n_points,
        "warning"             : warning,
    }