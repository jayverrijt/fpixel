#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="${BASE_DIR:-/home/jayv/fpixel}"
SERVER_DIR="${SERVER_DIR:-$BASE_DIR/server}"
SESSION_NAME="fpixel"

if screen -list | grep -q "\.${SESSION_NAME}[[:space:]]"; then
  echo "Server is already running in screen session '${SESSION_NAME}'!"
  echo "Use 'screen -r ${SESSION_NAME}' to view the console."
  exit 0
fi

cd "${SERVER_DIR}"
echo "Starting FPixel server in screen session '${SESSION_NAME}'..."
screen -dmS "${SESSION_NAME}" bash "${SERVER_DIR}/start.sh"
sleep 2

if screen -list | grep -q "\.${SESSION_NAME}[[:space:]]"; then
  echo "Server started successfully in background screen '${SESSION_NAME}'."
  echo "Attach anytime with: screen -r ${SESSION_NAME}"
else
  echo "Server failed to stay running. Check logs in ${SERVER_DIR}/logs/latest.log"
  exit 1
fi
