import logging
import os

import psycopg2
from ollama import Client as OllamaClient
from qdrant_client import QdrantClient

from models.schemas import DataQuality, EventOutcome, HistoricalAnalogue, HistoricalCoverage

logger = logging.getLogger("amrs.sidecar.historical")

QDRANT_COLLECTION = "market_events"
EMBED_MODEL = "nomic-embed-text"
SIMILARITY_THRESHOLD = 0.60

PG_CONFIG = {
    "host": os.getenv("POSTGRES_HOST"),
    "port": int(os.getenv("POSTGRES_PORT")),
    "dbname": os.getenv("POSTGRES_DB"),
    "user": os.getenv("POSTGRES_USER"),
    "password": os.getenv("POSTGRES_PASSWORD"),
}


def _embed(text: str, ollama: OllamaClient) -> list[float]:
    response = ollama.embeddings(model=EMBED_MODEL, prompt=text)
    return response["embedding"]


def _fetch_outcomes(event_id: str, conn) -> list[EventOutcome]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT ticker, window_days, return_pct, quality
            FROM event_outcomes
            WHERE event_id = %s
            ORDER BY ticker, window_days
            """,
            (event_id,),
        )
        rows = cur.fetchall()

    return [
        EventOutcome(
            ticker=row[0],
            window_days=row[1],
            return_pct=float(row[2]) if row[2] is not None else None,
            quality=DataQuality(row[3]),
        )
        for row in rows
    ]


def get_historical_analogues(
    event_text: str,
    qdrant: QdrantClient,
    ollama: OllamaClient,
) -> tuple[list[HistoricalAnalogue], HistoricalCoverage]:
    try:
        embedding = _embed(event_text, ollama)

        raw_results = qdrant.query_points(
            collection_name=QDRANT_COLLECTION,
            query=embedding,
            limit=5,
        ).points

        results = [r for r in raw_results if r.score >= SIMILARITY_THRESHOLD]

        if not results:
            return [], HistoricalCoverage.INSUFFICIENT

        conn = psycopg2.connect(**PG_CONFIG)
        analogues = []

        try:
            for point in results:
                payload = point.payload
                event_id = payload["event_id"]
                outcomes = _fetch_outcomes(event_id, conn)

                analogues.append(
                    HistoricalAnalogue(
                        event_id=event_id,
                        title=payload["title"],
                        date=payload["date"],
                        similarity_score=round(point.score, 4),
                        what_happened=payload["what_happened"],
                        outcomes=outcomes,
                    )
                )
        finally:
            conn.close()

        return analogues, HistoricalCoverage.SUFFICIENT

    except Exception:
        logger.exception("Failed to retrieve historical analogues")
        return [], HistoricalCoverage.INSUFFICIENT