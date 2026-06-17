FROM python:3.13.14-slim

WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*
RUN useradd -m appuser
USER appuser
COPY --chown=appuser:appuser requirements-sidecar.txt .

RUN pip install --no-cache-dir --user -r requirements-sidecar.txt \
        --extra-index-url https://download.pytorch.org/whl/cpu

# Ensure local bin is in PATH (for --user installs)
ENV PATH="/home/appuser/.local/bin:$PATH"
COPY --chown=appuser:appuser . .

EXPOSE 8000
CMD ["python", "-m", "uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]