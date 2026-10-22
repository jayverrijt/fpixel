#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# FPixel Network - Full Server Automated Update
# Updates the whole server (code, plugin, worlds, containers) safely with backups
# ==============================================================================

BASE_DIR="${BASE_DIR:-/home/jayv/fpixel}"
if [ ! -d "${BASE_DIR}" ] && [ -d "/Users/jayv/Projects/Fpixel2" ]; then
    BASE_DIR="/Users/jayv/Projects/Fpixel2"
fi

SERVER_DIR="${SERVER_DIR:-$BASE_DIR/server}"
SCRIPTS_DIR="${BASE_DIR}/scripts"
PLUGIN_DIR="${BASE_DIR}/plugin"
CONTAINER_NAME="${CONTAINER_NAME:-fpixel-minecraft}"

echo "=========================================="
echo " Starting FPixel Whole-Server Update"
echo " Timestamp: $(date +'%Y-%m-%d %H:%M:%S %Z')"
echo " Base Dir:  ${BASE_DIR}"
echo "=========================================="

# 1. Pre-update full backup
if [ -f "${SCRIPTS_DIR}/backup.sh" ]; then
    echo ">>> [1/5] Creating pre-update server snapshot..."
    bash "${SCRIPTS_DIR}/backup.sh"
else
    echo ">>> [1/5] scripts/backup.sh not found, skipping pre-update snapshot."
fi

# 2. Fetch git updates if available
echo ">>> [2/5] Updating codebase from Git repository..."
cd "${BASE_DIR}"
if [ -d ".git" ]; then
    git fetch origin main || true
    # Only reset if clean or requested
    if git diff --quiet && git diff --cached --quiet; then
        git merge origin/main --ff-only || true
    else
        echo "Local uncommitted changes detected; preserving local working tree."
    fi
fi

# 3. Build FPixelCore plugin JAR
echo ">>> [3/5] Compiling FPixelCore plugin..."
if [ -f "${PLUGIN_DIR}/gradlew" ]; then
    chmod +x "${PLUGIN_DIR}/gradlew"
    (
        cd "${PLUGIN_DIR}"
        ./gradlew build -x test --no-daemon
    )
    if [ -f "${PLUGIN_DIR}/build/libs/FPixelCore-1.0.0.jar" ]; then
        mkdir -p "${SERVER_DIR}/plugins"
        cp -f "${PLUGIN_DIR}/build/libs/FPixelCore-1.0.0.jar" "${SERVER_DIR}/plugins/FPixelCore.jar"
        echo "  -> Deployed FPixelCore.jar to ${SERVER_DIR}/plugins/FPixelCore.jar"
    fi
fi

# 4. Check dependencies and runtime files
echo ">>> [4/5] Verifying dependencies & file permissions..."
chmod +x "${SCRIPTS_DIR}"/*.sh 2>/dev/null || true
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    docker exec "${CONTAINER_NAME}" chown -R 1000:1000 /data 2>/dev/null || true
fi

# 5. Restart services or reload
echo ">>> [5/5] Applying update to runtime..."
if command -v docker >/dev/null 2>&1 && [ -f "${BASE_DIR}/docker-compose.yml" ]; then
    cd "${BASE_DIR}"
    if docker compose ps -q 2>/dev/null | grep -q .; then
        echo "Restarting Docker containers..."
        docker compose restart
    fi
fi

echo "=========================================="
echo " Whole-Server Update Completed Successfully!"
echo "=========================================="
