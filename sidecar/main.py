import logging
import os

from contextlib import asynccontextmanager
from fastapi import FastAPI
from ollama import Client as OllamaClient
from qdrant_client import QdrantClient
from dotenv import load_dotenv
from services.contradiction import get_nli_pipeline

load_dotenv()

from routes.analytics import router as analytics_router
from routes.nli import router as nli_router

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)

logger = logging.getLogger("amrs.sidecar")

@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.qdrant = QdrantClient(
        host=os.getenv("QDRANT_HOST"),
        port=int(os.getenv("QDRANT_PORT")),
    )
    app.state.ollama = OllamaClient(host=os.getenv("OLLAMA_HOST"))

    logger.info("Warming up NLI pipeline...")
    get_nli_pipeline()
    logger.info("NLI pipeline warmed and ready")

    logger.info("Sidecar clients initialised")
    yield
    logger.info("Sidecar shutting down")


app = FastAPI(title="AMRS Python Sidecar", lifespan=lifespan)

app.include_router(analytics_router)
app.include_router(nli_router)