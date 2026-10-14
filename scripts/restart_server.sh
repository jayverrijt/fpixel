#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Restarting FPixel server..."
bash "${DIR}/stop_server.sh"
sleep 3
bash "${DIR}/start_server.sh"
