"""
tests/test_forecast.py
Tests unitaires — ai-sav-forecast
Lance avec : pytest tests/ -v
"""
import pytest
import pandas as pd
from datetime import datetime, timedelta


# ── Données de test ──────────────────────────────────────────────
def make_historique(n_days: int = 100):
    """Génère un historique de test réaliste."""
    class DataPoint:
        def __init__(self, date, total, critique):
            self.date     = date
            self.total    = total
            self.critique = critique

    points = []
    base   = datetime(2025, 6, 25)

    for i in range(n_days):
        date = base + timedelta(days=i)
        # Weekends → 0
        if date.weekday() >= 5:
            total    = 0
            critique = 0
        else:
            # Pattern lundi fort
            factor   = 1.4 if date.weekday() == 0 else 1.0
            total    = int(20 * factor + (i % 7))
            critique = int(total * 0.15)

        points.append(DataPoint(
            date     = date.strftime("%Y-%m-%d"),
            total    = total,
            critique = critique,
        ))

    return points


# ── Tests preprocessor ───────────────────────────────────────────
class TestPreprocessor:

    def test_build_dataframe(self):
        from app.preprocessor import build_dataframe
        historique = make_historique(50)
        df = build_dataframe(historique)
        assert "ds" in df.columns
        assert "y_total" in df.columns
        assert "y_critique" in df.columns
        assert len(df) == 50

    def test_fill_date_range(self):
        from app.preprocessor import build_dataframe, fill_date_range
        historique = make_historique(30)
        df      = build_dataframe(historique)
        df_full = fill_date_range(df)
        # Doit couvrir toute la plage de dates
        expected_days = (df["ds"].max() - df["ds"].min()).days + 1
        assert len(df_full) == expected_days

    def test_cap_outliers(self):
        from app.preprocessor import build_dataframe, fill_date_range, cap_outliers
        historique = make_historique(50)
        df      = build_dataframe(historique)
        df_full = fill_date_range(df)
        df_cap  = cap_outliers(df_full)
        # Aucune valeur ne dépasse P95
        p95 = df_full["y_total"].quantile(0.95)
        assert df_cap["y_total"].max() <= p95 + 1

    def test_granularity_daily(self):
        from app.preprocessor import build_dataframe, fill_date_range, cap_outliers, get_granularity
        historique = make_historique(100)
        df      = build_dataframe(historique)
        df_full = fill_date_range(df)
        df_cap  = cap_outliers(df_full)
        granularity = get_granularity(df_cap)
        assert granularity == "daily"

    def test_granularity_weekly_insufficient(self):
        from app.preprocessor import build_dataframe, fill_date_range, cap_outliers, get_granularity
        historique = make_historique(40)  # Peu de données
        df      = build_dataframe(historique)
        df_full = fill_date_range(df)
        df_cap  = cap_outliers(df_full)
        granularity = get_granularity(df_cap)
        assert granularity == "weekly"

    def test_no_negative_values(self):
        from app.preprocessor import preprocess
        historique = make_historique(100)
        _, df_total, df_critique, _ = preprocess(historique)
        assert (df_total["y"] >= 0).all()
        assert (df_critique["y"] >= 0).all()


# ── Tests forecaster ─────────────────────────────────────────────
class TestForecaster:

    def test_prophet_model_builds(self):
        from app.forecaster import build_prophet_model
        model = build_prophet_model("daily")
        assert model is not None

    def test_predict_returns_positive(self):
        from app.preprocessor import preprocess
        from app.forecaster import predict
        historique = make_historique(100)
        _, df_total, _, granularity = preprocess(historique)
        forecast, _ = predict(df_total, granularity, 30, "total")
        # Prédictions positives
        assert (forecast["yhat"] >= 0).all()
        assert (forecast["yhat_lower"] >= 0).all()

    def test_run_forecast_response(self):
        from app.preprocessor import preprocess
        from app.forecaster import run_forecast
        historique = make_historique(100)
        _, df_total, df_critique, granularity = preprocess(historique)
        result = run_forecast(df_total, df_critique, granularity, 30, "LAHDIY")
        assert "predictions_total" in result
        assert "predictions_critique" in result
        assert "confidence" in result
        assert len(result["predictions_total"]) == 30

    def test_pct_critique_between_0_and_100(self):
        from app.preprocessor import preprocess
        from app.forecaster import run_forecast
        historique = make_historique(100)
        _, df_total, df_critique, granularity = preprocess(historique)
        result = run_forecast(df_total, df_critique, granularity, 30, "LAHDIY")
        if result["pct_critique_prevu"] is not None:
            assert 0 <= result["pct_critique_prevu"] <= 100


# ── Tests API ────────────────────────────────────────────────────
class TestAPI:

    @pytest.fixture
    def client(self):
        from fastapi.testclient import TestClient
        from app.main import app
        return TestClient(app)

    def test_health(self, client):
        response = client.get("/api/v1/health")
        assert response.status_code == 200
        assert response.json()["status"] == "ok"

    def test_forecast_endpoint(self, client):
        historique = make_historique(100)
        payload = {
            "agence"        : "LAHDIY",
            "horizon_jours" : 30,
            "historique"    : [
                {"date": p.date, "total": p.total, "critique": p.critique}
                for p in historique
            ]
        }
        response = client.post("/api/v1/forecast", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert "predictions_total" in data
        assert "predictions_critique" in data
        assert data["agence"] == "LAHDIY"

    def test_forecast_insufficient_data(self, client):
        payload = {
            "agence"        : "LAHDIY",
            "horizon_jours" : 30,
            "historique"    : [
                {"date": "2025-07-14", "total": 10, "critique": 2}
            ]
        }
        response = client.post("/api/v1/forecast", json=payload)
        assert response.status_code == 422

    def test_info_endpoint(self, client):
        response = client.get("/api/v1/forecast/info?agence=LAHDIY")
        assert response.status_code == 200
        assert response.json()["agence"] == "LAHDIY"

    def test_info_unknown_agence(self, client):
        response = client.get("/api/v1/forecast/info?agence=UNKNOWN")
        assert response.status_code == 404
