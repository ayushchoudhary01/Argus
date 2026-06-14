import logging

from fastapi import APIRouter, HTTPException

from models.schemas import ContradictionMap, NLIRequest
from services.contradiction import detect_contradictions

logger = logging.getLogger("amrs.sidecar.routes.nli")

router = APIRouter()


@router.post("/nli", response_model=ContradictionMap)
async def nli(request: NLIRequest) -> ContradictionMap:
    try:
        return detect_contradictions(
            assumptions=request.assumptions,
            causal_chains=request.causal_chains,
        )
    except Exception:
        logger.exception("Failed to process NLI request")
        raise HTTPException(status_code=500, detail="NLI pipeline failed")