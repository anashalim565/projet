from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, Depends
from app.config import settings
from app.logging import get_logger
from app.classifier import BCSClassifier
from app.schemas import (
    ClassifyRequest, ClassifyResponse,
    BatchClassifyRequest, BatchClassifyResponse,
)
from app.security import verify_internal_api_key
from fastapi.middleware.cors import CORSMiddleware


logger = get_logger(settings.SERVICE_NAME)

# ════════════════════════════════
# CLASSIFIER — singleton
# ════════════════════════════════

classifier = BCSClassifier(
    model_path=settings.MODEL_PATH,
    device=settings.DEVICE,
    model_version=settings.MODEL_VERSION,
)


# ════════════════════════════════
# LIFESPAN
# ════════════════════════════════

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info(f"Starting {settings.SERVICE_NAME} v{settings.SERVICE_VERSION}")
    logger.info(f"ENV={settings.ENVIRONMENT} PORT={settings.PORT}")
    logger.info(f"MODEL_PATH={settings.MODEL_PATH} DEVICE={settings.DEVICE}")
    classifier.load()
    logger.info("Classifier prêt")
    yield
    logger.info(f"Arrêt {settings.SERVICE_NAME}")


# ════════════════════════════════
# APP
# ════════════════════════════════

app = FastAPI(
    title=settings.SERVICE_NAME,
    version=settings.SERVICE_VERSION,
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)


# ════════════════════════════════
# ENDPOINTS
# ════════════════════════════════

@app.get("/api/v1/health")
def health():
    return {
        "status": "ok",
        "service": settings.SERVICE_NAME,
        "version": settings.SERVICE_VERSION,
        "environment": settings.ENVIRONMENT,
    }


@app.get("/api/v1/model/info")
def model_info():
    return {
        "service": settings.SERVICE_NAME,
        "version": settings.SERVICE_VERSION,
        "model_path": settings.MODEL_PATH,
        "model_version": settings.MODEL_VERSION,
        "device": settings.DEVICE,
        "model_loaded": classifier.is_loaded,
    }


@app.post("/api/v1/classify", response_model=ClassifyResponse)
def classify(request: ClassifyRequest, _: None = Depends(verify_internal_api_key)):
    if not classifier.is_loaded:
        raise HTTPException(status_code=503, detail="Modèle non chargé")
    result = classifier.classify(
        commentaire=request.commentaire,
        ticket_number=request.ticket_number,
        use_distlbert=request.use_distlbert,
    )
    return ClassifyResponse(**result)


@app.post("/api/v1/classify/batch", response_model=BatchClassifyResponse)
def classify_batch(request: BatchClassifyRequest):
    if not classifier.is_loaded:
        raise HTTPException(status_code=503, detail="Modèle non chargé")
    tickets = [t.model_dump() for t in request.tickets]
    results = classifier.classify_batch(tickets)
    critiques = sum(1 for r in results if r["is_critique"])
    return BatchClassifyResponse(
        total=len(results),
        critiques=critiques,
        non_critiques=len(results) - critiques,
        results=[ClassifyResponse(**r) for r in results],
    )

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # pour test local seulement
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)