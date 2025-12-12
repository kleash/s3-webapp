# S3 Browser Webapp

A Spring Boot + Angular web UI for browsing and managing objects on any S3-compatible storage (MinIO, Ceph, AWS S3, etc.). Buckets and credentials come from config; the UI supports folder-style navigation, search, copy/move, deletes, downloads, and folder size aggregation.

## Tech + Ports
- Backend: Java 17, Spring Boot 3.2 (port `9080`)
- Frontend: Angular 17 (dev server `9071`, production assets served by backend)
- Storage: MinIO (S3-compatible) on ports `9070` (API) / `9072` (console)
- Build: Maven for backend, Angular CLI for frontend

## Project layout
```
backend/   # Spring Boot API
frontend/  # Angular client
scripts/   # helper scripts (infra, seed, demo, e2e)
e2e/       # Playwright end-to-end tests
Dockerfile
docker-compose.yml
```

## Configuration
Buckets are defined under `s3.buckets` in `backend/src/main/resources/application.yaml` (and `application-docker.yaml` for compose). Each entry requires:
```
- id: logical identifier (used by API/UI)
  name: display name
  bucketName: actual S3 bucket
  endpointUrl: http://<host>:<port>
  accessKey: ...
  secretKey: ...
  region: us-east-1
  pathStyleAccess: true|false
```
Update these values for your environment; `pathStyleAccess` should be true for MinIO.

### LDAP configuration
Authentication and authorization are driven by LDAP. Example config (kebab-case keys):
```yaml
security:
  ldap:
    url: "ldap://ldap.example.com:389"
    base-dn: "DC=example,DC=com"
    bind-dn: "CN=ldap-reader,OU=ServiceAccounts,DC=example,DC=com"
    bind-password: "changeMe"
    user-search-base: "OU=Users,DC=example,DC=com"
    user-search-filter: "(sAMAccountName={0})"
    read-only-groups:
      - "CN=S3_ReadOnly,OU=Groups,DC=example,DC=com"
    read-write-groups:
      - "CN=S3_ReadWrite,OU=Groups,DC=example,DC=com"
    read-only-users: []
    read-write-users: []
    ignore-ssl-validation: true   # dev/non-prod only
    no-role-policy: DENY          # reject auth if no role matches
    embedded:
      enabled: true               # starts in-memory LDAP for dev/e2e
      port: 1389
      seed-ldif: "classpath:ldap/seed.ldif"
```
- Authentication uses a service account to find the user DN (`sAMAccountName`) and then binds as the user to validate the password.
- Roles are derived from `memberOf` group DNs and/or explicit usernames. If both read-only and read-write match, read-write wins.
- Default behavior if nothing matches is to deny access.
- `ignore-ssl-validation` exists for convenience; production deployments should use proper TLS trust.

## Prerequisites
- Java 17+
- Maven 3.9+
- Node 18+/npm + Angular CLI (17.x)
- Docker (for MinIO, docker-compose, e2e, and packaged runs)

## Quick start (one-command demo)
```
./scripts/demo.sh
```
This will:
1) Build the Angular app, copy it into backend static resources
2) Build the Spring Boot jar
3) Start MinIO + backend via docker compose
4) Seed buckets with sample data
5) Print the app URL (`http://localhost:9080`)

Login with the dev LDAP users:
- `alice` / `password1` (Read-only)
- `bob` / `password1` (Read & write)

Stop everything with:
```
./scripts/stop-infra.sh
```

## Manual run / dev loop
1) Start storage + backend container:
```
./scripts/start-infra.sh   # MinIO on 9070/9072, backend on 9080
```
   - Or only MinIO for local dev: `docker compose up -d minio`
2) Seed sample data (idempotent): `./scripts/seed-data.sh`
3) Dev the backend locally (auto-restart):
```
cd backend
mvn spring-boot:run
```
4) Dev the frontend with live reload on port 9071 (CORS already open for 9071):
```
cd frontend
npm install
npm start   # http://localhost:9071
```
   For production build into the backend: `npm run build:prod && cp -r dist/frontend/* ../backend/src/main/resources/static/`

## Docker compose
```
docker compose up -d --build
```
- MinIO: http://localhost:9070 (console http://localhost:9072)
- Backend + served UI: http://localhost:9080
Uses `application-docker.yaml` which points S3 endpoints at the MinIO container.

## Scripts
- `scripts/start-infra.sh` – build + start MinIO and backend containers
- `scripts/stop-infra.sh` – tear everything down
- `scripts/seed-data.sh` – create/clear buckets `logs` and `backups`, upload sample objects
- `scripts/demo.sh` – full build + start + seed + health wait
- `scripts/e2e.sh` – build stack, seed data, run Playwright suite headlessly, then stop
- `scripts/stop-app.sh` – convenience wrapper to stop the running stack (same as stop-infra)

All scripts are idempotent; re-running seed will recreate objects.

## Documentation
- Backend architecture: `docs/backend-architecture.md`
- Feature guide: `docs/features.md`

## Seeded data (try these paths)
Bucket `logs`:
- `app/2025/01/01/app.log`
- `app/2025/01/02/debug.json`
- `app/2025/01/02/trade_2025_01.csv`
- `readme.txt` at root

Bucket `backups`:
- `db/2025/01/01/dump.bin` (random bytes)
- `files/config.json`
- `orphan.bin` at root

These cover deep folders, mixed file types, wildcard search (`trade_2025_*.csv`), and copy/move/delete flows.

## REST API (high level)
- `GET /api/buckets` – configured buckets
- `GET /api/buckets/{id}/objects?prefix=&pageToken=` – list folders/files (virtual folders by prefix, pagination)
- `GET /api/buckets/{id}/search?query=trade_2025_*.csv&prefix=app/2025/` – wildcard search (prefix optional)
- `GET /api/buckets/{id}/objects/download?key=...` – download
- `POST /api/buckets/{id}/objects/copy|move` – body `{sourceKey,targetKey,overwrite}`
- `DELETE /api/buckets/{id}/objects` – body `{keys:[...]}`
- `DELETE /api/buckets/{id}/folders` – body `{prefix:".../"}` (recursive delete)
- `GET /api/buckets/{id}/folders/size?prefix=.../` – aggregate size + object count

## Testing
- Backend unit/integration: `mvn -f backend/pom.xml test`
- Frontend unit tests: `npm test` (Angular/Karma; requires Chrome/Chromium) – build sanity check via `npm run build`
- End-to-end: `./scripts/e2e.sh` (builds images, seeds MinIO + embedded LDAP, runs Playwright against live stack)

## Search & size notes
- Search is a simple wildcard match over listed keys (case-insensitive). For large buckets it performs paged listings client-side; consider tightening prefixes for performance.
- Folder size iterates all objects under the prefix; for very large trees this can take time.

## Ports reminder
All services are bound to the 9070–9080 range per requirements: MinIO 9070/9072, frontend dev 9071, backend 9080.
