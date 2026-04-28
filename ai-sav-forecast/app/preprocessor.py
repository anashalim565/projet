"""
preprocessor.py
Nettoyage des données avant Prophet.
Résout les 6 problèmes identifiés sur les données BCS.
"""
import pandas as pd
import numpy as np
from pathlib import Path
from typing import List, Tuple, Optional
from app.config import settings
from app.logging import get_logger

logger = get_logger(__name__)


# ── Jours fériés Maroc — chargés depuis CSV ─────────────────────
def _load_holidays() -> pd.DataFrame:
    """
    Charge les jours fériés depuis data/holidays_ma.csv.
    Format CSV : ds,holiday  — Couvre 2025-2028.
    Pour ajouter une année : ajouter les lignes dans le CSV,
    pas besoin de toucher au code.
    """
    csv_path = Path(__file__).parent.parent / "data" / "holidays_ma.csv"

    if not csv_path.exists():
        logger.warning(
            f"Fichier jours fériés introuvable : {csv_path} "
            f"— Prophet fonctionnera sans fériés"
        )
        return pd.DataFrame({
            "ds"     : pd.Series(dtype="datetime64[ns]"),
            "holiday": pd.Series(dtype="str")
        })

    df = pd.read_csv(csv_path)
    df["ds"] = pd.to_datetime(df["ds"])
    logger.info(f"Jours fériés chargés : {len(df)} dates depuis {csv_path.name}")
    return df


MOROCCO_HOLIDAYS = _load_holidays()


# ── Fonctions principales ────────────────────────────────────────

def build_dataframe(historique: list) -> pd.DataFrame:
    """
    Convertit la liste de TicketDataPoint en DataFrame.
    Colonnes résultat : ds, y_total, y_critique
    """
    records = [
        {
            "ds"         : pd.to_datetime(p.date),
            "y_total"    : p.total,
            "y_critique" : p.critique
        }
        for p in historique
    ]
    df = pd.DataFrame(records)
    df = df.sort_values("ds").reset_index(drop=True)
    df = df.drop_duplicates(subset="ds")

    logger.info(
        f"DataFrame construit : {len(df)} points "
        f"({df['ds'].min().date()} → {df['ds'].max().date()})"
    )
    return df


def fill_date_range(df: pd.DataFrame) -> pd.DataFrame:
    """
    Crée un calendrier complet entre la première et dernière date.
    Stratégie par type de jour :
      - Weekend  → 0   (banque fermée, normal)
      - Férié    → 0   (banque fermée, normal)
      - Gap <= 3 → interpolation linéaire
      - Gap > 3  → NaN (Prophet ignore ces périodes)
    """
    full_range = pd.date_range(df["ds"].min(), df["ds"].max(), freq="D")
    full_df    = pd.DataFrame({"ds": full_range})
    full_df    = full_df.merge(df, on="ds", how="left")

    # Marquer weekends et fériés
    full_df["is_weekend"] = full_df["ds"].dt.dayofweek >= 5
    full_df["is_holiday"] = full_df["ds"].isin(MOROCCO_HOLIDAYS["ds"])

    # Weekends et fériés → 0
    mask_zero = full_df["is_weekend"] | full_df["is_holiday"]
    full_df.loc[mask_zero, ["y_total", "y_critique"]] = 0

    # Jours ouvrés manquants → interpoler si petit gap, NaN si grand
    for col in ["y_total", "y_critique"]:
        is_nan    = full_df[col].isna() & ~mask_zero
        gap_id    = (is_nan != is_nan.shift()).cumsum()
        gap_sizes = is_nan.groupby(gap_id).transform("sum")

        # Interpoler tous les gaps d'abord
        full_df[col] = full_df[col].interpolate(method="linear")

        # Remettre NaN pour les grands gaps
        large_gap_mask = is_nan & (gap_sizes > settings.MAX_GAP_INTERPOLATE)
        full_df.loc[large_gap_mask, col] = np.nan

    n_nan = full_df["y_total"].isna().sum()
    logger.info(
        f"Calendrier complet : {len(full_df)} jours | "
        f"NaN (grands gaps ignorés par Prophet) : {n_nan}"
    )
    return full_df


def cap_outliers(df: pd.DataFrame) -> pd.DataFrame:
    """
    Cap les valeurs extrêmes au percentile 95.
    Uniquement sur les jours ouvrés non-fériés.
    Évite que le pic de juillet biaise les prédictions.
    """
    workdays = df[
        ~df["is_weekend"] &
        ~df["is_holiday"] &
        df["y_total"].notna()
    ]

    for col in ["y_total", "y_critique"]:
        if workdays[col].empty:
            continue
        p95      = workdays[col].quantile(settings.OUTLIER_PERCENTILE)
        n_capped = (df[col] > p95).sum()
        df[col]  = df[col].clip(upper=p95)
        if n_capped > 0:
            logger.info(f"Outliers cappés [{col}] : {n_capped} valeurs > {p95:.0f}")

    return df


def to_prophet_df(df: pd.DataFrame, target: str) -> pd.DataFrame:
    """
    Convertit en format attendu par Prophet : colonnes ds + y.
    Supprime les NaN et les négatifs.
    """
    prophet_df = df[["ds", target]].rename(columns={target: "y"})
    prophet_df = prophet_df.dropna(subset=["y"])
    prophet_df = prophet_df[prophet_df["y"] >= 0]
    return prophet_df.reset_index(drop=True)


def get_granularity(df: pd.DataFrame) -> str:
    """
    Détermine la granularité selon le nombre de jours ouvrés disponibles.
    DAILY  si >= MIN_POINTS_DAILY (70 jours)
    WEEKLY sinon
    """
    workdays_with_data = df[
        ~df["is_weekend"] &
        ~df["is_holiday"] &
        df["y_total"].notna() &
        (df["y_total"] > 0)
    ]
    n = len(workdays_with_data)

    if n >= settings.MIN_POINTS_DAILY:
        logger.info(f"Granularité : DAILY ({n} points ouvrés)")
        return "daily"
    else:
        logger.warning(
            f"Granularité : WEEKLY — {n} points ouvrés "
            f"(min requis={settings.MIN_POINTS_DAILY})"
        )
        return "weekly"


def resample_weekly(df: pd.DataFrame) -> pd.DataFrame:
    """
    Agrège les données en semaines.
    Utilisé pour LAHRYA qui a moins de 70 jours ouvrés.
    """
    df_copy = df.copy().set_index("ds")
    weekly  = df_copy[["y_total", "y_critique"]].resample("W-MON").sum()
    weekly  = weekly.reset_index()
    weekly.columns      = ["ds", "y_total", "y_critique"]
    weekly["is_weekend"] = False
    weekly["is_holiday"] = False

    logger.info(f"Resampled weekly : {len(weekly)} semaines")
    return weekly


def get_warning(
    n_points   : int,
    granularity: str,
    mape       : Optional[float] = None
) -> Optional[str]:
    """
    Retourne un message d'avertissement affiché sur le dashboard
    si les données sont insuffisantes ou la précision limitée.
    """
    if granularity == "weekly":
        return (
            f"Données insuffisantes pour prédiction journalière "
            f"({n_points} jours ouvrés < {settings.MIN_POINTS_DAILY} requis). "
            f"Prédiction hebdomadaire — à utiliser comme tendance indicative."
        )
    if mape is not None and mape > settings.MAPE_MEDIUM_THRESHOLD:
        return (
            f"Précision limitée (MAPE={mape:.1f}%) "
            f"— à utiliser comme tendance indicative."
        )
    return None


def preprocess(
    historique: list
) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame, str]:
    """
    Pipeline complet de preprocessing.
    Appelé par main.py avant run_forecast().

    Étapes :
      1. Construire DataFrame depuis historique
      2. Calendrier complet + gestion gaps
      3. Cap outliers au P95
      4. Déterminer granularité (daily / weekly)
      5. Resampler en hebdomadaire si nécessaire
      6. Préparer DataFrames Prophet (total + critique)

    Returns:
        df_full             : DataFrame complet
        df_prophet_total    : DataFrame Prophet volume total
        df_prophet_critique : DataFrame Prophet volume critique
        granularity         : 'daily' ou 'weekly'
    """
    logger.info("=== Début preprocessing ===")

    df      = build_dataframe(historique)
    df_full = fill_date_range(df)
    df_full = cap_outliers(df_full)

    granularity = get_granularity(df_full)

    if granularity == "weekly":
        df_full = resample_weekly(df_full)

    df_prophet_total    = to_prophet_df(df_full, "y_total")
    df_prophet_critique = to_prophet_df(df_full, "y_critique")

    logger.info(
        f"=== Preprocessing terminé | granularity={granularity} | "
        f"points_total={len(df_prophet_total)} | "
        f"points_critique={len(df_prophet_critique)} ==="
    )

    return df_full, df_prophet_total, df_prophet_critique, granularity