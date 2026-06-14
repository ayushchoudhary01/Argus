import logging
from datetime import datetime, timedelta

import yfinance as yf

from models.schemas import TechnicalContext

logger = logging.getLogger("amrs.sidecar.technical")


def get_technical_context() -> TechnicalContext:
    try:
        end = datetime.utcnow()
        start = end - timedelta(days=60)

        df = yf.download("SPY", start=start, end=end, progress=False, auto_adjust=True)

        if df.empty or len(df) < 15:
            logger.warning("SPY data unavailable — returning empty TechnicalContext")
            return TechnicalContext()

        closes = df["Close"].squeeze()

        delta = closes.diff()
        gain = delta.clip(lower=0).rolling(14).mean()
        loss = (-delta.clip(upper=0)).rolling(14).mean()
        rs = gain / loss
        rsi = round(float(100 - (100 / (1 + rs.iloc[-1]))), 2)

        sma_20 = closes.rolling(20).mean()
        slope = float(sma_20.iloc[-1]) - float(sma_20.iloc[-5])
        trend = "upward" if slope > 0 else "downward"
        if abs(rsi - 70) < 5:
            trend = "near_overbought"
        elif abs(rsi - 30) < 5:
            trend = "near_oversold"

        return TechnicalContext(spy_rsi_14=rsi, spy_trend=trend)

    except Exception:
        logger.exception("Failed to compute technical context")
        return TechnicalContext()