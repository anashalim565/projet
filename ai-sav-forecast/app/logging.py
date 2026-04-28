"""
logging.py
Configuration des logs structurés — ai-sav-forecast
Même pattern que ai-sav-criticality
"""
import logging
import sys
from app.config import settings


def get_logger(name: str = settings.SERVICE_NAME) -> logging.Logger:
    """
    Retourne un logger configuré.
    Usage : logger = get_logger(__name__)
    """
    logger = logging.getLogger(name)

    if logger.handlers:
        return logger

    logger.setLevel(logging.DEBUG if settings.DEBUG else logging.INFO)

    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG if settings.DEBUG else logging.INFO)

    formatter = logging.Formatter(
        fmt="%(asctime)s %(levelname)s %(name)s - %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S"
    )
    handler.setFormatter(formatter)
    logger.addHandler(handler)

    return logger
