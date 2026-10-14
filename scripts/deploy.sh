#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="${BASE_DIR:-/home/jayv/fpixel}"
SERVER_DIR="${SERVER_DIR:-$BASE_DIR/server}"
SCRIPTS_DIR="${BASE_DIR}/scripts"

echo "=========================================="
echo " Starting FPixel Automated Deploy"
echo " Date: $(date)"
echo " Base Dir: ${BASE_DIR}"
echo "=========================================="

# 1. Run automated backup before modifying anything
if [ -f "${SCRIPTS_DIR}/backup.sh" ]; then
  echo ">>> [1/5] Running Pre-Deploy Backup..."
  bash "${SCRIPTS_DIR}/backup.sh"
else
  echo ">>> [1/5] backup.sh not found, skipping pre-deploy backup."
fi

# 2. Update repository to latest origin/main
echo ">>> [2/5] Updating repository from origin/main..."
cd "${BASE_DIR}"
git fetch origin main
git reset --hard origin/main

# 3. Ensure essential runtime jars are present
echo ">>> [3/5] Verifying runtime dependencies..."
mkdir -p "${SERVER_DIR}/plugins"

# Check paper.jar
if [ ! -f "${SERVER_DIR}/paper.jar" ]; then
  if [ -f "${BASE_DIR}/survival/paper.jar" ]; then
    echo "Copying existing paper.jar from survival/..."
    cp "${BASE_DIR}/survival/paper.jar" "${SERVER_DIR}/paper.jar"
  elif [ -f "${SERVER_DIR}/versions/26.3/paper-26.3.jar" ]; then
    echo "Copying paper-26.3.jar to paper.jar..."
    cp "${SERVER_DIR}/versions/26.3/paper-26.3.jar" "${SERVER_DIR}/paper.jar"
  fi
fi

# Check Geyser-Spigot.jar
if [ ! -f "${SERVER_DIR}/plugins/Geyser-Spigot.jar" ]; then
  if [ -f "${BASE_DIR}/survival/plugins/Geyser-Spigot.jar" ]; then
    echo "Copying Geyser-Spigot.jar from survival/plugins/..."
    cp "${BASE_DIR}/survival/plugins/Geyser-Spigot.jar" "${SERVER_DIR}/plugins/Geyser-Spigot.jar"
  fi
fi

# Check floodgate-spigot.jar
if [ ! -f "${SERVER_DIR}/plugins/floodgate-spigot.jar" ]; then
  if [ -f "${BASE_DIR}/survival/plugins/floodgate-spigot.jar" ]; then
    echo "Copying floodgate-spigot.jar from survival/plugins/..."
    cp "${BASE_DIR}/survival/plugins/floodgate-spigot.jar" "${SERVER_DIR}/plugins/floodgate-spigot.jar"
  fi
fi

# 4. Set execution permissions
echo ">>> [4/5] Setting permissions..."
chmod +x "${BASE_DIR}"/scripts/*.sh "${SERVER_DIR}"/start.sh "${BASE_DIR}"/plugin/gradlew 2>/dev/null || true

# 5. Notify or reload running server if screen session exists
echo ">>> [5/5] Checking running server status..."
if screen -list | grep -q "fpixel"; then
  echo "Active 'fpixel' screen session detected. Reloading FPixelCore..."
  screen -S fpixel -X stuff "fpixel reload\n" 2>/dev/null || true
  screen -S fpixel -X stuff "say §a[FPixel CI/CD] Server successfully updated to latest build!§r\n" 2>/dev/null || true
else
  echo "No active 'fpixel' screen session. Server is offline or running under another service."
fi

# 6. Run post-deploy backup confirmation
if [ -f "${SCRIPTS_DIR}/backup.sh" ]; then
  echo ">>> Running Post-Deploy Backup Snapshot..."
  bash "${SCRIPTS_DIR}/backup.sh"
fi

echo "=========================================="
echo " FPixel Deploy Completed Successfully! "
echo "=========================================="
