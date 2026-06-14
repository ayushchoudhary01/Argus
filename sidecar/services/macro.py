import logging
from datetime import datetime, timedelta

import yfinance as yf

from models.schemas import MacroContext

logger = logging.getLogger("amrs.sidecar.macro")


def get_macro_context() -> MacroContext:
    try:
        end = datetime.utcnow()
        start = end - timedelta(days=30)

        tnx = yf.download("^TNX", start=start, end=end, progress=False, auto_adjust=True)
        irx = yf.download("^IRX", start=start, end=end, progress=False, auto_adjust=True)
        dxy = yf.download("DX-Y.NYB", start=start, end=end, progress=False, auto_adjust=True)

        if tnx.empty or irx.empty:
            logger.warning("Yield curve data unavailable — returning empty MacroContext")
            return MacroContext()

        ten_year = float(tnx["Close"].squeeze().iloc[-1])
        three_month = float(irx["Close"].squeeze().iloc[-1])
        yield_curve_slope = round(ten_year - three_month, 4)

        dxy_momentum = None
        if not dxy.empty and len(dxy) >= 5:
            dxy_closes = dxy["Close"].squeeze()
            slope = float(dxy_closes.iloc[-1]) - float(dxy_closes.iloc[-5])
            dxy_momentum = "rising" if slope > 0 else "falling"

        return MacroContext(yield_curve_slope=yield_curve_slope, dxy_momentum=dxy_momentum)

    except Exception:
        logger.exception("Failed to compute macro context")
        return MacroContext()