import logging
from datetime import datetime, timedelta

import yfinance as yf

from models.schemas import VolatilityContext

logger = logging.getLogger("amrs.sidecar.volatility")


def get_volatility_context() -> VolatilityContext:
    try:
        end = datetime.utcnow()
        start = end - timedelta(days=45)

        df = yf.download("^VIX", start=start, end=end, progress=False, auto_adjust=True)

        if df.empty or len(df) < 2:
            logger.warning("VIX data unavailable — returning empty VolatilityContext")
            return VolatilityContext()

        closes = df["Close"].squeeze()
        current_vix = round(float(closes.iloc[-1]), 4)
        window = closes.iloc[-30:] if len(closes) >= 30 else closes
        percentile = round(float((window < current_vix).sum() / len(window) * 100), 2)

        return VolatilityContext(vix_level=current_vix, percentile_30d=percentile)

    except Exception:
        logger.exception("Failed to compute volatility context")
        return VolatilityContext()