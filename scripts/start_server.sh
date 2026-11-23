#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="${BASE_DIR:-/home/jayv/fpixel}"
cd "${BASE_DIR}"

echo "Starting FPixel services via Docker Compose..."
docker compose up -d
docker compose ps
