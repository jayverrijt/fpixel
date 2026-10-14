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
if [ ! -f "${SERVER_DIR}/paper.jar" ] || [ $(stat -c%s "${SERVER_DIR}/paper.jar" 2>/dev/null || echo 0) -lt 40000000 ]; then
  if [ -f "/home/jayv/fpixel-af2727b702412a9bbd1b5ac3af6817f9107ac91b/survival/paper.jar" ]; then
    echo "Copying bootstrap paper.jar from fpixel-af2727b702412a9bbd1b5ac3af6817f9107ac91b/survival..."
    cp "/home/jayv/fpixel-af2727b702412a9bbd1b5ac3af6817f9107ac91b/survival/paper.jar" "${SERVER_DIR}/paper.jar"
  elif [ -f "/shared/fpixel/survival/paper.jar" ]; then
    echo "Copying bootstrap paper.jar from /shared/fpixel/survival..."
    cp "/shared/fpixel/survival/paper.jar" "${SERVER_DIR}/paper.jar"
  else
    echo "Downloading Paper 26.3..."
    curl -sSL "https://fill-data.papermc.io/v1/objects/09d38bfa3a7100e4d80554b9a964dfbf124f1d62c9ceabe482769e8a1098b4c3/paper-26.3-183.jar" -o "${SERVER_DIR}/paper.jar" || true
  fi
fi

# Check Geyser-Spigot.jar
if [ ! -f "${SERVER_DIR}/plugins/Geyser-Spigot.jar" ]; then
  if [ -f "/shared/fpixel/survival/plugins/Geyser-Spigot.jar" ]; then
    echo "Copying Geyser-Spigot.jar from /shared/fpixel..."
    cp "/shared/fpixel/survival/plugins/Geyser-Spigot.jar" "${SERVER_DIR}/plugins/Geyser-Spigot.jar"
  else
    echo "Downloading latest Geyser-Spigot.jar..."
    curl -sSL "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot" -o "${SERVER_DIR}/plugins/Geyser-Spigot.jar" || true
  fi
fi

# Check floodgate-spigot.jar
if [ ! -f "${SERVER_DIR}/plugins/floodgate-spigot.jar" ]; then
  if [ -f "/shared/fpixel/survival/plugins/floodgate.jar" ]; then
    echo "Copying floodgate.jar from /shared/fpixel..."
    cp "/shared/fpixel/survival/plugins/floodgate.jar" "${SERVER_DIR}/plugins/floodgate-spigot.jar"
  else
    echo "Downloading latest floodgate-spigot.jar..."
    curl -sSL "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot" -o "${SERVER_DIR}/plugins/floodgate-spigot.jar" || true
  fi
fi

# 4. Set execution permissions
echo ">>> [4/5] Setting permissions..."
chmod +x "${BASE_DIR}"/scripts/*.sh "${SERVER_DIR}"/start.sh "${BASE_DIR}"/plugin/gradlew 2>/dev/null || true

# 5. Build and launch Docker containers (Minecraft + Web Stats)
echo ">>> [5/6] Deploying Docker services (Minecraft + Web Stats)..."
# Stop legacy screen session if active to avoid port conflict
if screen -list | grep -q "\.fpixel[[:space:]]"; then
  echo "Gracefully stopping legacy screen session 'fpixel'..."
  screen -S fpixel -X stuff "stop\n" 2>/dev/null || true
  sleep 4
  screen -S fpixel -X quit 2>/dev/null || true
fi

# Clean up stale session.locks
rm -f "${SERVER_DIR}"/world/session.lock "${SERVER_DIR}"/Urban/session.lock "${SERVER_DIR}"/Galaxy/session.lock 2>/dev/null || true

cd "${BASE_DIR}"
docker compose build
docker compose up -d
docker compose restart

# 6. Run post-deploy backup confirmation
if [ -f "${SCRIPTS_DIR}/backup.sh" ]; then
  echo ">>> Running Post-Deploy Backup Snapshot..."
  bash "${SCRIPTS_DIR}/backup.sh"
fi

echo "=========================================="
echo " FPixel Deploy Completed Successfully! "
echo "=========================================="
