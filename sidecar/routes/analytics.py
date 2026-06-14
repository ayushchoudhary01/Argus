import logging

from fastapi import APIRouter, HTTPException, Request
from ollama import Client as OllamaClient
from qdrant_client import QdrantClient

from models.schemas import AnalyticsRequest, MarketContext
from services.anomaly import get_anomaly_flags
from services.historical import get_historical_analogues
from services.macro import get_macro_context
from services.technical import get_technical_context
from services.volatility import get_volatility_context

logger = logging.getLogger("amrs.sidecar.routes.analytics")

router = APIRouter()


@router.post("/analytics", response_model=MarketContext)
async def analytics(request: AnalyticsRequest, req: Request) -> MarketContext:
    qdrant: QdrantClient = req.app.state.qdrant
    ollama: OllamaClient = req.app.state.ollama

    try:
        volatility = get_volatility_context()
        technical = get_technical_context()
        macro = get_macro_context()
        anomaly_flags = get_anomaly_flags()
        historical_similarity, historical_coverage = get_historical_analogues(
            event_text=request.event,
            qdrant=qdrant,
            ollama=ollama,
        )

        return MarketContext(
            volatility=volatility,
            technical=technical,
            macro=macro,
            anomaly_flags=anomaly_flags,
            historical_coverage=historical_coverage,
            historical_similarity=historical_similarity,
        )

    except Exception:
        logger.exception("Failed to process analytics request")
        raise HTTPException(status_code=500, detail="Analytics pipeline failed")