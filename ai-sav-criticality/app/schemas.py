from pydantic import BaseModel, Field
from typing import List, Optional


class ClassifyRequest(BaseModel):
    commentaire: str = Field(..., min_length=3, max_length=2000)
    ticket_number: Optional[str] = None
    use_distlbert: bool = True


class ClassifyResponse(BaseModel):
    model_config = {"protected_namespaces": ()}  # ← ajoute cette ligne
    ticket_number: Optional[str]
    label: str                    # CRITIQUE ou NON_CRITIQUE
    label_value: int              # 1 ou 0
    confiance: float              # 0.0 → 100.0
    is_critique: bool
    sentiment: str                # NEGATIF / NEUTRE / POSITIF
    sentiment_score: float        # -1.0 → +1.0
    urgence: int                  # 0 → 3
    emotion: str
    signaux: List[str]
    modele_utilise: str
    model_version: str


class BatchClassifyRequest(BaseModel):
    tickets: List[ClassifyRequest]


class BatchClassifyResponse(BaseModel):
    total: int
    critiques: int
    non_critiques: int
    results: List[ClassifyResponse]