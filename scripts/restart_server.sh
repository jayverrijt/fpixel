#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="${BASE_DIR:-/home/jayv/fpixel}"
cd "${BASE_DIR}"

echo "Restarting FPixel services via Docker Compose..."
docker compose restart
docker compose ps
