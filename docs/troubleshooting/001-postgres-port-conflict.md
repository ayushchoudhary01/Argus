# PostgreSQL Port Conflict With Local Installation

## Symptom
psycopg2.OperationalError: connection to server at "127.0.0.1", port 5432 failed: FATAL: password authentication failed
Credentials are correct but authentication consistently fails.

## Cause
A local PostgreSQL installation is running on the same machine and occupying port 5432. Docker maps its PostgreSQL container to the same port. The Python client connects to the local installation instead of the Docker container, which has different credentials.

## Diagnosis
```powershell
netstat -ano | findstr 5432
Get-Process -Id <PID>
```
If two processes are listening on 5432 and one is not Docker, this is the cause.

## Fix
Change the Docker PostgreSQL port mapping in `docker-compose.yml`:
```yaml
ports:
  - "5433:5432"
```
Add to `.env`:
POSTGRES_PORT=5433
Restart containers:
```powershell
docker-compose down -v
docker-compose up -d
```