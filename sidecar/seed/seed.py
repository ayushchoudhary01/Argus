import json
import logging
import os
import sys
from datetime import datetime, timedelta
from enum import Enum

import psycopg2
import yfinance as yf
from ollama import Client as OllamaClient
from qdrant_client import QdrantClient
from qdrant_client.models import Distance, PointStruct, VectorParams
from dotenv import load_dotenv
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "..", "..", ".env"))

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("amrs.seed")

# print("DB USER:", os.getenv("POSTGRES_USER"))
# print("DB PASS:", os.getenv("POSTGRES_PASSWORD"))
EVENTS_PATH = os.path.join(os.path.dirname(__file__), "events.json")

PG_CONFIG = {
    "host": os.getenv("POSTGRES_HOST"),
    "port": int(os.getenv("POSTGRES_HOST_PORT")),
    "dbname": os.getenv("POSTGRES_DB"),
    "user": os.getenv("POSTGRES_USER"),
    "password": "x",
}

QDRANT_HOST = os.getenv("QDRANT_HOST", "localhost")
QDRANT_PORT = int(os.getenv("QDRANT_PORT", 6333))
QDRANT_COLLECTION = "market_events"

OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://localhost:11434")
EMBED_MODEL = "nomic-embed-text"
EMBED_DIMENSION = 768

RETURN_WINDOWS = [1, 10, 30]


class DataQuality(str, Enum):
    VERIFIED = "VERIFIED"
    UNAVAILABLE = "UNAVAILABLE"


SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS events (
    id          VARCHAR(64) PRIMARY KEY,
    date        DATE        NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT        NOT NULL,
    what_happened TEXT      NOT NULL,
    assets      TEXT[]      NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS event_outcomes (
    id          SERIAL PRIMARY KEY,
    event_id    VARCHAR(64) NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    ticker      VARCHAR(32) NOT NULL,
    window_days INTEGER     NOT NULL,
    return_pct  NUMERIC(8, 4),
    quality     VARCHAR(32) NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (event_id, ticker, window_days)
);
"""

def init_db(conn) -> None:
    with conn.cursor() as cur:
        cur.execute(SCHEMA_SQL)
    conn.commit()
    logger.info("Database schema initialised")


def fetch_return(ticker: str, event_date: datetime.date, window_days: int) -> tuple[float | None, DataQuality]:
    try:
        start = event_date
        end = event_date + timedelta(days=window_days + 10)

        df = yf.download(ticker, start=start, end=end, progress=False, auto_adjust=True)

        if df.empty or len(df) < 2:
            return None, DataQuality.UNAVAILABLE

        price_start = float(df["Close"].iloc[0].values[0])
        target_index = min(window_days, len(df) - 1)
        price_end = float(df["Close"].iloc[target_index].values[0])

        return_pct = ((price_end - price_start) / price_start) * 100
        return round(float(return_pct), 4), DataQuality.VERIFIED

    except Exception as e:
        logger.warning(f"Failed to fetch {ticker} for {event_date} +{window_days}d — {e}")
        return None, DataQuality.UNAVAILABLE


def embed_text(ollama: OllamaClient, text: str) -> list[float]:
    response = ollama.embeddings(model=EMBED_MODEL, prompt=text)
    return response["embedding"]


def init_qdrant(qdrant: QdrantClient) -> None:
    existing = [c.name for c in qdrant.get_collections().collections]
    if QDRANT_COLLECTION not in existing:
        qdrant.create_collection(
            collection_name=QDRANT_COLLECTION,
            vectors_config=VectorParams(size=EMBED_DIMENSION, distance=Distance.COSINE),
        )
        logger.info(f"Qdrant collection '{QDRANT_COLLECTION}' created")
    else:
        logger.info(f"Qdrant collection '{QDRANT_COLLECTION}' already exists")


def seed_event(conn, qdrant: QdrantClient, ollama: OllamaClient, event: dict) -> None:
    event_id = event["id"]
    event_date = datetime.strptime(event["date"], "%Y-%m-%d").date()

    # ── Insert into events table ──
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO events (id, date, title, description, what_happened, assets)
            VALUES (%s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO NOTHING
            """,
            (
                event_id,
                event_date,
                event["title"],
                event["description"],
                event["what_happened"],
                event["assets"],
            ),
        )
    conn.commit()

    for ticker in event["assets"]:
        for window in RETURN_WINDOWS:
            return_pct, quality = fetch_return(ticker, event_date, window)
            with conn.cursor() as cur:
                cur.execute(
                    """
                    INSERT INTO event_outcomes (event_id, ticker, window_days, return_pct, quality)
                    VALUES (%s, %s, %s, %s, %s)
                    ON CONFLICT (event_id, ticker, window_days) DO NOTHING
                    """,
                    (event_id, ticker, window, return_pct, quality.value),
                )
            conn.commit()
            logger.info(f"{event_id} | {ticker} | {window}d | {return_pct} | {quality.value}")

    embedding = embed_text(ollama, event["description"])
    qdrant.upsert(
        collection_name=QDRANT_COLLECTION,
        points=[
            PointStruct(
                id=abs(hash(event_id)) % (2**63),
                vector=embedding,
                payload={
                    "event_id": event_id,
                    "date": event["date"],
                    "title": event["title"],
                    "what_happened": event["what_happened"],
                    "assets": event["assets"],
                },
            )
        ],
    )
    logger.info(f"{event_id} — embedded and stored in Qdrant")

def main() -> None:
    logger.info("AMRS seed process starting")

    with open(EVENTS_PATH, "r") as f:
        events = json.load(f)
    logger.info(f"Loaded {len(events)} events from {EVENTS_PATH}")

    conn = psycopg2.connect(**PG_CONFIG)
    qdrant = QdrantClient(host=QDRANT_HOST, port=QDRANT_PORT)
    ollama = OllamaClient(host=OLLAMA_HOST)

    init_db(conn)
    init_qdrant(qdrant)

    failed = []
    for i, event in enumerate(events, 1):
        logger.info(f"Processing event {i}/{len(events)} — {event['title']}")
        try:
            seed_event(conn, qdrant, ollama, event)
        except Exception as e:
            logger.error(f"Failed to seed {event['id']} — {e}")
            failed.append(event["id"])

    conn.close()

    if failed:
        logger.warning(f"Seed completed with {len(failed)} failures: {failed}")
        sys.exit(1)
    else:
        logger.info("Seed completed successfully — all events processed")

if __name__ == "__main__":
    main()