# Qdrant Healthcheck Reporting Unhealthy

## Symptom
`docker-compose ps` shows `amrs-qdrant` as `unhealthy` despite Qdrant being fully operational.

## Cause
The Docker healthcheck uses `curl` to probe the Qdrant health endpoint. The official Qdrant image does not include `curl`, so the healthcheck command fails every time, causing Docker to mark the container unhealthy.

## Diagnosis
```powershell
Invoke-WebRequest -Uri http://localhost:6333/healthz -UseBasicParsing
```
If this returns `200 OK`, Qdrant is healthy and the issue is only the healthcheck probe.

## Fix
Remove the healthcheck block from the Qdrant service in `docker-compose.yml` entirely. Qdrant does not need a Docker healthcheck for local development.