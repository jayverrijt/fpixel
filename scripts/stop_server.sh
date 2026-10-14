#!/usr/bin/env bash
set -euo pipefail

SESSION_NAME="fpixel"

if ! screen -list | grep -q "\.${SESSION_NAME}[[:space:]]"; then
  echo "No active screen session '${SESSION_NAME}' found."
  exit 0
fi

echo "Stopping FPixel server gracefully..."
screen -S "${SESSION_NAME}" -X stuff "say §c[Server] Server is stopping in 5 seconds...§r\n"
sleep 5
screen -S "${SESSION_NAME}" -X stuff "stop\n"

# Wait up to 30 seconds for server process to terminate
for i in {1..30}; do
  if ! screen -list | grep -q "\.${SESSION_NAME}[[:space:]]"; then
    echo "Server stopped successfully."
    exit 0
  fi
  sleep 1
done

echo "Server did not shut down in time, killing session..."
screen -S "${SESSION_NAME}" -X quit || true
echo "Session terminated."
