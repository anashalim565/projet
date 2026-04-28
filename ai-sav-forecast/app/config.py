"""
config.py
Configuration centralisée — ai-sav-forecast
"""
from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):

    # ── Service
    SERVICE_NAME   : str   = "ai-sav-forecast"
    VERSION        : str   = "1.0.0"
    ENV            : str   = "development"
    PORT           : int   = 8000
    DEBUG          : bool  = False

    # ── Prophet
    HORIZON_JOURS_DEFAULT : int   = 30
    HORIZON_JOURS_MAX     : int   = 60
    SEASONALITY_MODE      : str   = "multiplicative"
    CHANGEPOINT_PRIOR     : float = 0.05
    SEASONALITY_PRIOR     : float = 10.0
    INTERVAL_WIDTH        : float = 0.80

    DATABASE_URL: str = "postgresql://postgres:n@localhost:5432/aisav_db"

    # ── Seuils qualité
    MIN_POINTS_DAILY      : int   = 70
    MIN_POINTS_WEEKLY     : int   = 20
    MAPE_HIGH_THRESHOLD   : float = 20.0
    MAPE_MEDIUM_THRESHOLD : float = 35.0

    # ── Outliers
    OUTLIER_PERCENTILE    : float = 0.95
    MAX_GAP_INTERPOLATE   : int   = 3

    # ── Agences — List[str] correctement typé pour pydantic-settings
    AGENCES_VALIDES       : List[str] = ["JARMOUN", "LAHDIY", "LAHRYA"]

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
