from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", case_sensitive=False, extra="ignore")

    SERVICE_NAME: str = "ai-sav-criticality"
    SERVICE_VERSION: str = "2.0.0"
    ENVIRONMENT: str = "development"
    PORT: int = 8001
    MODEL_PATH: str = "./model/distilbert_bcs"
    FALLBACK_MODEL_PATH: str = "./model/pipeline_ml.pkl"
    MODEL_VERSION: str = "v7"
    DEVICE: str = "auto"
    LOG_LEVEL: str = "INFO"

    internal_api_key: str

    
settings = Settings()

