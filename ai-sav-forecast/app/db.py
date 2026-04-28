"""
db.py — accès PostgreSQL pour lire les prédictions stockées par ForecastJob.
Utilise psycopg2 directement (pas de SQLAlchemy) pour rester léger.
"""
import psycopg2
import psycopg2.extras
from typing import List, Optional
from app.config import settings
from app.logging import get_logger

logger = get_logger(__name__)


def get_connection():
    return psycopg2.connect(settings.DATABASE_URL)


def get_stored_forecast(agence: str) -> Optional[dict]:
    agence = agence.upper()
    sql = """
        SELECT forecast_date, type, yhat, yhat_lower, yhat_upper,
               pct_critique, mape, confidence, granularity, generated_at
        FROM forecasts
        WHERE agence = %s
        ORDER BY type, forecast_date ASC
    """
    try:
        conn = get_connection()
        with conn:
            with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
                cur.execute(sql, (agence,))
                rows = cur.fetchall()
        conn.close()

        if not rows:
            return None

        predictions_total    = []
        predictions_critique = []
        confidence   = None
        granularity  = None
        mape         = None
        pct_critique = None
        generated_at = None

        for row in rows:
            point = {
                "date"       : row["forecast_date"].strftime("%Y-%m-%d"),
                "yhat"       : float(row["yhat"]) if row["yhat"] else 0.0,
                "yhat_lower" : float(row["yhat_lower"]) if row["yhat_lower"] else 0.0,
                "yhat_upper" : float(row["yhat_upper"]) if row["yhat_upper"] else 0.0,
                "is_weekend" : row["forecast_date"].weekday() >= 5,
                "is_holiday" : False,
            }

            if row["type"] == "TOTAL":
                predictions_total.append(point)
                confidence   = row["confidence"]
                granularity  = row["granularity"]
                mape         = float(row["mape"]) if row["mape"] else None
                pct_critique = float(row["pct_critique"]) if row["pct_critique"] else None
                if generated_at is None:
                    generated_at = row["generated_at"]
            elif row["type"] == "CRITIQUE":
                predictions_critique.append(point)

        return {
            "agence"              : agence,
            "granularity"         : granularity or "daily",
            "confidence"          : confidence or "MEDIUM",
            "mape"                : mape,
            "pct_critique_prevu"  : pct_critique,
            "predictions_total"   : predictions_total,
            "predictions_critique": predictions_critique,
            "generated_at"        : generated_at.strftime("%Y-%m-%dT%H:%M:%S") if generated_at else "",
            "warning"             : None,
        }

    except Exception as e:
        logger.error(f"Erreur lecture DB | agence={agence} | {e}")
        raise