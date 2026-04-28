import re
import torch
import unicodedata
import logging
import numpy as np
from pathlib import Path
from typing import Optional
#from nltk.corpus import stopwords
#from nltk.tokenize import word_tokenize
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import nltk

logger = logging.getLogger(__name__)
STOP_FR = {
    "le", "la", "les", "un", "une", "des", "de", "du", "d",
    "au", "aux", "et", "ou", "où", "en", "dans", "sur", "sous",
    "avec", "sans", "ce", "cet", "cette", "ces", "qui", "que",
    "quoi", "dont", "je", "tu", "il", "elle", "nous", "vous",
    "ils", "elles", "a", "à", "est", "sont", "été", "être",
    "avoir", "bonjour", "merci", "madame", "monsieur", "bien",
    "necessaire", "suite", "plus", "votre", "notre",
    "cordialement"
}

#STOP_FR = set(stopwords.words("french"))
#STOP_FR.update({
#    "bonjour", "merci", "madame", "monsieur", "bien",
#    "necessaire", "suite", "avoir", "etre", "plus",
#    "votre", "notre", "vous", "nous", "cordialement"
#})

MOTS_CRITIQUES = {
    "retrait", "non_servi", "debite", "debit", "remboursement",
    "fraude", "pirate", "double", "prelevement", "injustifie",
    "annulation", "opposition", "bloque", "urgent", "vole",
    "perdu", "rembourser", "restituer", "irregulier", "non_recu",
    "disparu", "erreur", "contestation", "tort", "conteste",
    "expire", "echoue",
}

MOTS_URGENCE = [
    "urgent", "urgence", "immediat", "immediatement",
    "relance", "malgre", "vain", "inutile", "insatisfait",
    "inacceptable", "scandaleux", "inadmissible", "honte",
    "non servi", "pas servi", "pas recu", "rien recu",
    "bloque", "expire", "perdu", "disparu",
    "pirate", "fraude", "vole", "opposition", "annulation",
]

SENT_NEG = {
    "fraude": -0.95, "pirate": -1.0, "vole": -0.95,
    "non_servi": -0.9, "debite": -0.6, "double": -0.65,
    "bloque": -0.75, "perdu": -0.8, "injustifie": -0.85,
    "tort": -0.75, "remboursement": -0.55, "restituer": -0.65,
    "urgent": -0.85, "inacceptable": -0.9, "scandaleux": -0.9,
    "erreur": -0.55, "probleme": -0.55, "expire": -0.6,
    "retard": -0.55, "encore": -0.5, "malgre": -0.55,
    "inquiet": -0.6, "peur": -0.7, "stressant": -0.6,
    "stress": -0.6, "autorise": -0.5,
}

SENT_POS = {
    "satisfait": 0.6, "parfait": 0.65, "excellent": 0.7,
    "resolu": 0.6, "rapide": 0.4, "efficace": 0.5,
}

INTENSIFIERS = {
    "tres": 1.6, "vraiment": 1.5, "totalement": 1.5,
    "extremement": 1.6, "absolument": 1.4,
}


def clean_text(text: str) -> str:
    if not text:
        return ""
    text = str(text).lower()
    text = unicodedata.normalize("NFD", text)
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    text = re.sub(r"http\S+|www\.\S+|\S+@\S+", " ", text)
    text = re.sub(r"\b(ref|n°|num|code)\s*[\w\d-]+\b", " ", text)
    text = re.sub(r"(\d+[.,]?\d*)\s*(dhs|mad|dh|dirham|eur|€)", r"montant_\1", text)
    text = re.sub(r"\b(\d{3,6})\b", r"montant_\1", text)
    text = re.sub(r"[^\w\s'\\-]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


#def tokenize_clean(text: str) -> list:
#    tokens = word_tokenize(text, language="french")
#    return [t for t in tokens if len(t) > 2 and t not in STOP_FR]
def tokenize_clean(text: str) -> list:
    tokens = re.findall(r"\b[\w'-]+\b", text.lower())
    return [t for t in tokens if len(t) > 2 and t not in STOP_FR]

def get_sentiment_score(text: str) -> float:
    words = str(text).lower().split()
    score = 0.0
    for i, w in enumerate(words):
        mult = INTENSIFIERS.get(words[i - 1], 1.0) if i > 0 else 1.0
        if w in SENT_NEG:
            score += SENT_NEG[w] * mult
        elif w in SENT_POS:
            score += SENT_POS[w] * mult
    return round(max(-1.0, min(1.0, score / max(1, len(words) / 10))), 4)


def get_sentiment_label(score: float) -> str:
    if score < -0.15:
        return "NEGATIF"
    if score > 0.1:
        return "POSITIF"
    return "NEUTRE"


def get_urgence(text: str) -> int:
    t = str(text).lower()
    s = 0
    if any(w in t for w in ["fraude", "pirate", "vole", "escroquerie"]):
        s += 3
    elif any(w in t for w in ["non servi", "pas servi", "pas recu",
                               "debite", "double", "bloque",
                               "expire", "perdu", "opposition"]):
        s += 2
    if any(w in t for w in ["urgent", "urgence", "inacceptable",
                              "malgre", "relance", "inadmissible"]):
        s += 1
    return min(3, s)


def get_emotion(text: str) -> str:
    t = str(text).lower()
    if any(w in t for w in ["fraude", "pirate", "vole"]):
        return "PEUR/ALERTE"
    if any(w in t for w in ["inacceptable", "scandaleux", "inadmissible"]):
        return "COLERE"
    if any(w in t for w in ["encore", "malgre", "vain", "inutile"]):
        return "FRUSTRATION"
    if any(w in t for w in ["perdu", "disparu", "expire", "bloque"]):
        return "INQUIETUDE"
    if any(w in t for w in ["comment", "information", "renseignement"]):
        return "NEUTRE/INFO"
    return "NEUTRE"


def extract_signals(commentaire: str, clean: str) -> list:
    signals = []
    sc_crit = sum(
        1 for m in MOTS_CRITIQUES
        if m.replace("_", " ") in clean or m in clean
    )
    sc_urg = sum(1 for m in MOTS_URGENCE if m in clean)
    if sc_crit > 0:
        signals.append(f"mots_critiques:{sc_crit}")
    if sc_urg > 0:
        signals.append(f"urgence:{sc_urg}")
    if re.search(r"\d{3,6}", commentaire):
        signals.append("montant_mentionne")
    if re.search(r"\d{1,2}/\d{1,2}/\d{2,4}", commentaire):
        signals.append("date_referencee")
    if any(n in clean.split() for n in ["non", "pas", "rien", "jamais"]):
        signals.append("negation")
    return signals or ["aucun_signal_critique"]


class BCSClassifier:

    def __init__(self, model_path: str,
                 device: str = "auto",
                 model_version: str = "v1"):
        self.model_path = model_path
        self.model_version = model_version
        self.device = self._resolve_device(device)
        self.tokenizer = None
        self.model = None
        self._loaded = False

    @staticmethod
    def _resolve_device(device: str) -> torch.device:
        if device == "auto":
            return torch.device("cuda" if torch.cuda.is_available() else "cpu")
        return torch.device(device)

    def load(self):
        model_path = Path(self.model_path)
        if not model_path.exists():
            raise RuntimeError(f"Modèle non trouvé : {model_path}")

        logger.info(f"Chargement DistilBERT multilingue depuis {model_path}")
        self.tokenizer = AutoTokenizer.from_pretrained(str(model_path))
        self.model = AutoModelForSequenceClassification.from_pretrained(
            str(model_path)
        )
        self.model.to(self.device)
        self.model.eval()
        logger.info(f"DistilBERT chargé sur {self.device}")
        self._loaded = True

    @property
    def is_loaded(self) -> bool:
        return self._loaded

    def classify(self, commentaire: str,
                 ticket_number: Optional[str] = None,
                 use_distlbert: bool = True) -> dict:

        clean = clean_text(commentaire)
        tokens = tokenize_clean(clean)
        text_tokens = " ".join(tokens)

        sent_score = get_sentiment_score(clean)
        urgence = get_urgence(commentaire)
        emotion = get_emotion(commentaire)
        signaux = extract_signals(commentaire, clean)

        enc = self.tokenizer(
            commentaire,
            add_special_tokens=True,
            max_length=256,
            padding="max_length",
            truncation=True,
            return_tensors="pt",
        )
        with torch.no_grad():
            out = self.model(
                input_ids=enc["input_ids"].to(self.device),
                attention_mask=enc["attention_mask"].to(self.device),
            )
        probs = torch.softmax(out.logits, dim=1).cpu().numpy()[0]
        pred_idx = int(probs.argmax())
        confiance = float(probs[pred_idx])

        return {
            "ticket_number": ticket_number,
            "label": "CRITIQUE" if pred_idx == 1 else "NON_CRITIQUE",
            "label_value": pred_idx,
            "confiance": round(confiance * 100, 1),
            "is_critique": pred_idx == 1,
            "sentiment": get_sentiment_label(sent_score),
            "sentiment_score": sent_score,
            "urgence": urgence,
            "emotion": emotion,
            "signaux": signaux,
            "modele_utilise": "DistilBERT-multilingual",
            "model_version": self.model_version,
        }

    def classify_batch(self, tickets: list) -> list:
        return [
            self.classify(
                commentaire=t["commentaire"],
                ticket_number=t.get("ticket_number"),
            )
            for t in tickets
        ]