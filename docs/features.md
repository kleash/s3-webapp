# Platform Features

## Introduction
The S3 Browser platform lets ops/support engineers and developers browse, search, and manage objects stored in any S3-compatible backend (MinIO, Ceph, AWS S3, etc.). It provides a clean Angular UI backed by a Spring Boot API, with scripts to spin up a full demo or run e2e regression tests.

## Feature overview
- Bucket management and configuration
- Folder & file browsing
- Search
- File & folder operations
- Bulk operations
- Folder size & statistics
- Download & inspection
- Configuration and environment
- Seed data & demo mode
- End-to-end regression tests

## Detailed features

### Bucket management
- [x] Load multiple buckets from `application.yaml` (id, name, endpoint, credentials, region, path-style flag)
- [x] UI bucket selector; default to first bucket

### Folder & file browsing
- [x] Virtual folders derived from object prefixes and delimiter `/`
- [x] Breadcrumb navigation for current prefix
- [x] Pagination support (`nextPageToken`) for large listings
- [x] Displays name, type, size, last modified, content type

### Search
- [x] Wildcard search (`*`) against keys/names, case-insensitive
- [x] Optional prefix scoping (“current folder only”)
- [x] Results reuse listing structure
- _Note:_ Search is client-side over listed keys; narrow prefixes for very large buckets

### File & folder operations
- [x] Single object copy/move with overwrite toggle
- [x] Single object delete
- [x] Folder delete (recursive by prefix)
- [x] Folder size aggregation (total bytes + object count)
- [x] Folder copy/move by prefix (deep structures preserved, overwrite-aware, per-object error reporting)

### Bulk operations (multi-select)
- [x] Row checkboxes + “Select all” in listings
- [x] Bulk delete for mixed selections (files and folder prefixes)
- [x] Bulk copy and move for multiple objects (per-item target keys) with overwrite control
- [x] Bulk copy and move for selected folders via prefix-based operations (supports deep trees; skips conflicts when overwrite=false)
- [x] Success/failure summaries surfaced in UI status bar

### Folder size & statistics
- [x] Per-prefix total size and object count
- [x] Human-readable display in UI

### Download & inspection
- [x] Download any object with correct `Content-Type` and `Content-Disposition`
- [x] Metadata shown in table (size/last modified/type)

### Configuration and environment
- [x] S3-compatible endpoints configurable (custom URL, credentials, region, path-style)
- [x] CORS enabled for dev hosts (9071/9080 by default)
- [x] Docker profile wired for docker-compose deployments

### Seed data & demo mode
- [x] `scripts/seed-data.sh` creates buckets `logs`, `backups` with nested folders, mixed file types/sizes, root-level files
- [x] `scripts/demo.sh` builds frontend+backend, starts MinIO + backend, seeds data, and prints access URL
- [x] Ports confined to 9070–9080 range

### End-to-end regression tests
- [x] Playwright suite exercises bucket load, folder navigation, search, download, bulk copy, single copy/move/delete, folder move, and folder size
- [x] `scripts/e2e.sh` builds images, starts compose stack, seeds data, runs tests headlessly, and tears down

## Usage examples
- **Select a bucket:** Choose from the dropdown at top; the root listing loads automatically.
- **Navigate folders:** Click “Open” on a folder row or breadcrumb segments to move up/down.
- **Perform a bulk delete:** Check multiple rows (files and/or folders), click “Bulk delete”. The view refreshes with a summary.
- **Move a folder:** Select a folder row (checkbox), enter a target prefix in the bulk bar (e.g., `root/app/2025/05/`), click “Bulk move”. Contents are moved preserving substructure; conflicts are skipped when overwrite is off.
- **Check folder size:** Enter prefix in the side panel “Folder prefix” box and click “Folder size” to show bytes and object count.
- **Run demo:** `./scripts/demo.sh` builds, starts MinIO + backend, seeds data, and serves the UI at `http://localhost:9080`.
- **Run e2e tests:** `./scripts/e2e.sh` brings up the stack, seeds, runs Playwright, and stops containers.

## Scripts and tests
- `start-infra.sh` / `stop-infra.sh` / `stop-app.sh`: manage compose stack
- `seed-data.sh`: idempotent MinIO seeding with realistic nested data
- `demo.sh`: one-command build + start + seed
- `e2e.sh`: full regression pipeline using docker-compose + Playwright
- Playwright suite mirrors the feature list above to catch regressions in core flows
