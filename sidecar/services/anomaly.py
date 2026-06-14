import logging
from datetime import datetime, timedelta

import yfinance as yf
import numpy as np

logger = logging.getLogger("amrs.sidecar.anomaly")


def get_anomaly_flags() -> list[str]:
    flags = []
    try:
        end = datetime.utcnow()
        start = end - timedelta(days=60)

        df = yf.download("SPY", start=start, end=end, progress=False, auto_adjust=True)

        if df.empty or len(df) < 30:
            logger.warning("SPY data unavailable — skipping anomaly detection")
            return flags

        volume = df["Volume"].squeeze()
        mean_vol = float(volume.iloc[:-1].mean())
        std_vol = float(volume.iloc[:-1].std())
        current_vol = float(volume.iloc[-1])

        if std_vol > 0 and (current_vol - mean_vol) / std_vol > 3:
            flags.append("volume_spike_3sigma")

        put_call = yf.download("^PCCE", start=end - timedelta(days=5), end=end, progress=False, auto_adjust=True)
        if not put_call.empty:
            pc_ratio = float(put_call["Close"].squeeze().iloc[-1])
            if pc_ratio > 1.0:
                flags.append("put_call_ratio_elevated")

    except Exception:
        logger.exception("Failed to compute anomaly flags")

    return flags