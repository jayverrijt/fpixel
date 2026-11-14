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
# Preserve persistent runtime files
if [ -f "${SERVER_DIR}/plugins/FPixelCore/player_worlds.json" ]; then
  cp "${SERVER_DIR}/plugins/FPixelCore/player_worlds.json" /tmp/player_worlds.json.bak 2>/dev/null || true
fi
cd "${BASE_DIR}"
git fetch origin main
git reset --hard origin/main
if [ -f /tmp/player_worlds.json.bak ]; then
  mkdir -p "${SERVER_DIR}/plugins/FPixelCore"
  cp -f /tmp/player_worlds.json.bak "${SERVER_DIR}/plugins/FPixelCore/player_worlds.json" 2>/dev/null || true
  rm -f /tmp/player_worlds.json.bak
fi

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
    echo "Downloading Paper 26.3 build 227..."
    curl -sSL "https://fill-data.papermc.io/v1/objects/416b7d105b1d0192df4afa4b29d2800d9dbeda092a82fbc5b511248a54adde15/paper-26.3-227.jar" -o "${SERVER_DIR}/paper.jar" || true
  fi
fi

# Download/verify Geyser-Spigot.jar
echo "Verifying latest Geyser-Spigot.jar..."
curl -sSL "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot" -o "${SERVER_DIR}/plugins/Geyser-Spigot.jar" || true

# Ensure NetherNet transport is enabled in Geyser config
if [ -f "${SERVER_DIR}/plugins/Geyser-Spigot/config.yml" ]; then
  sed -i 's/transport: raknet/transport: nethernet/' "${SERVER_DIR}/plugins/Geyser-Spigot/config.yml" || true
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

# Ensure latest Survival World dimension is present from backups_survival
if [ ! -d "${SERVER_DIR}/survival/region" ] && [ ! -d "${SERVER_DIR}/world/dimensions/minecraft/survival/region" ]; then
  echo ">>> Restoring latest survival world dimension from backups_survival..."
  git fetch origin backups_survival 2>/dev/null || true
  mkdir -p /tmp/survival_restore_deploy
  git archive origin/backups_survival | tar -x -C /tmp/survival_restore_deploy 2>/dev/null || true
  if [ -d /tmp/survival_restore_deploy/dimensions/minecraft/survival ]; then
    mkdir -p "${SERVER_DIR}/survival"
    cp -a /tmp/survival_restore_deploy/dimensions/minecraft/survival/* "${SERVER_DIR}/survival/"
    if [ -f /tmp/survival_restore_deploy/world/level.dat ]; then
      cp -a /tmp/survival_restore_deploy/world/level.dat "${SERVER_DIR}/survival/"
    fi
    mkdir -p "${SERVER_DIR}/world/dimensions/minecraft/survival"
    cp -a /tmp/survival_restore_deploy/dimensions/minecraft/survival/* "${SERVER_DIR}/world/dimensions/minecraft/survival/"
    mkdir -p "${SERVER_DIR}/plugins/FPixelCore/survival_data"
    cp -a /tmp/survival_restore_deploy/plugins/FPixelCore/survival_data/* "${SERVER_DIR}/plugins/FPixelCore/survival_data/" 2>/dev/null || true
    echo "  -> Latest survival backup successfully deployed to server/survival."
  fi
  rm -rf /tmp/survival_restore_deploy
fi

# 4. Set execution permissions
echo ">>> [4/6] Setting permissions..."
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

cd "${BASE_DIR}"

# Gracefully stop running containers so PaperMC saves and releases memory/locks cleanly
echo "Gracefully stopping active Docker services for reload..."
docker compose down || true

# Seed authentic survival profiles to server/plugins/FPixelCore/survival_data WHILE server is down!
mkdir -p "${SERVER_DIR}/plugins/FPixelCore/survival_data"
if [ -d "${BASE_DIR}/server/survival_data_seed" ]; then
  echo ">>> Restoring authentic survival profiles from server/survival_data_seed..."
  cp -a "${BASE_DIR}/server/survival_data_seed/"* "${SERVER_DIR}/plugins/FPixelCore/survival_data/"
fi

# Clean up stale session.locks
rm -f "${SERVER_DIR}"/world/session.lock "${SERVER_DIR}"/survival/session.lock "${SERVER_DIR}"/arenas/session.lock "${SERVER_DIR}"/world/dimensions/minecraft/survival/session.lock "${SERVER_DIR}"/Urban/session.lock "${SERVER_DIR}"/Galaxy/session.lock 2>/dev/null || true

docker compose build
docker compose up -d --force-recreate --remove-orphans

echo ">>> [6/6] Verifying service initialization..."
sleep 8
docker compose ps
echo "--- Recent Minecraft Container Logs ---"
docker compose logs --tail=100 minecraft || true
echo "--- Recent Web Container Logs ---"
docker compose logs --tail=30 fpixel-web || true

if command -v nc >/dev/null 2>&1 && nc -z -w 3 127.0.0.1 25565 2>/dev/null; then
  echo ">>> [SUCCESS] Minecraft server is live and listening on port 25565!"
else
  echo ">>> [INFO] Minecraft server container is running; PaperMC is completing initialization."
fi

# 6. Run post-deploy backup confirmation
if [ -f "${SCRIPTS_DIR}/backup.sh" ]; then
  echo ">>> Running Post-Deploy Backup Snapshot..."
  bash "${SCRIPTS_DIR}/backup.sh"
fi

echo "=========================================="
echo " FPixel Deploy Completed Successfully! "
echo "=========================================="
