#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# FPixel Network - Restore Survival World from GitHub Daily Backup
# Restores the latest survival world backup from branch 'backups_survival'
# ==============================================================================

REPO_DIR="${REPO_DIR:-/home/jayv/fpixel}"
SERVER_DIR="${SERVER_DIR:-${REPO_DIR}/server}"
CONTAINER_NAME="${CONTAINER_NAME:-fpixel-minecraft}"
BRANCH_NAME="backups_survival"
REMOTE_NAME="origin"
TEMP_RESTORE_DIR="/tmp/fpixel_survival_restore_$(date +%s)"

echo "=========================================="
echo " Starting Survival World Restore"
echo " Timestamp: $(date +'%Y-%m-%d %H:%M:%S %Z')"
echo " Repository: ${REPO_DIR}"
echo " Source Branch: ${BRANCH_NAME}"
echo "=========================================="

# 1. Stop Minecraft container safely
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Stopping Minecraft server (${CONTAINER_NAME})..."
    docker stop "${CONTAINER_NAME}"
fi

# 2. Safety snapshot of current world before restore
SNAPSHOT_TIME="$(date +'%Y%m%d_%H%M%S')"
echo "Creating safety snapshot of current world state before restore..."
docker run --rm -v "${SERVER_DIR}:/server" alpine sh -c "
    mkdir -p /server/world_safety_snapshots
    if [ -d /server/world/dimensions/minecraft/survival ]; then
        cp -a /server/world/dimensions/minecraft/survival /server/world_safety_snapshots/survival_before_restore_${SNAPSHOT_TIME}
    fi
    if [ -d /server/world/players ]; then
        cp -a /server/world/players /server/world_safety_snapshots/players_before_restore_${SNAPSHOT_TIME}
    fi
    if [ -f /server/world/level.dat ]; then
        cp -a /server/world/level.dat /server/world_safety_snapshots/level.dat_before_restore_${SNAPSHOT_TIME}
    fi
    if [ -d /server/plugins/FPixelCore/survival_data ]; then
        cp -a /server/plugins/FPixelCore/survival_data /server/world_safety_snapshots/survival_data_before_restore_${SNAPSHOT_TIME}
    fi
"
echo "Safety snapshot saved to ${SERVER_DIR}/world_safety_snapshots/ (timestamp: ${SNAPSHOT_TIME})"

TARGET_REF="${1:-${REMOTE_NAME}/${BRANCH_NAME}}"

# 3. Fetch latest backup from GitHub
echo "Fetching latest backup from ${REMOTE_NAME}/${BRANCH_NAME}..."
cd "${REPO_DIR}"
git fetch "${REMOTE_NAME}" "${BRANCH_NAME}"

COMMIT_INFO="$(git log -n 1 --format='%h - %s (%ci)' "${TARGET_REF}")"
echo "Restoring target: ${TARGET_REF} (${COMMIT_INFO})"

# 4. Extract archive to temporary directory
mkdir -p "${TEMP_RESTORE_DIR}"
git archive "${TARGET_REF}" | tar -x -C "${TEMP_RESTORE_DIR}"

if [ -f "${TEMP_RESTORE_DIR}/BACKUP_INFO.md" ]; then
    echo "Backup Metadata:"
    head -n 10 "${TEMP_RESTORE_DIR}/BACKUP_INFO.md"
fi

# 5. Restore files into server directory with root permissions
echo "Restoring survival dimension, player profiles, and plugin storage..."
docker run --rm -v "${SERVER_DIR}:/server" -v "${TEMP_RESTORE_DIR}:/restore" alpine sh -c "
    # Restore survival dimension
    if [ -d /restore/dimensions/minecraft/survival ]; then
        rm -rf /server/world/dimensions/minecraft/survival
        mkdir -p /server/world/dimensions/minecraft
        cp -a /restore/dimensions/minecraft/survival /server/world/dimensions/minecraft/survival
        echo '  -> Restored dimensions/minecraft/survival'
    fi

    # Restore player profiles
    if [ -d /restore/world/players ]; then
        mkdir -p /server/world/players
        cp -a /restore/world/players/* /server/world/players/
        echo '  -> Restored world/players'
    fi

    # Restore level.dat
    if [ -f /restore/world/level.dat ]; then
        cp -a /restore/world/level.dat /server/world/level.dat
        echo '  -> Restored world/level.dat'
    fi

    # Restore FPixelCore survival data
    if [ -d /restore/plugins/FPixelCore/survival_data ]; then
        mkdir -p /server/plugins/FPixelCore/survival_data
        cp -a /restore/plugins/FPixelCore/survival_data/* /server/plugins/FPixelCore/survival_data/
        echo '  -> Restored plugins/FPixelCore/survival_data'
    fi

    # Ensure correct ownership
    chown -R root:root /server/world/dimensions/minecraft/survival 2>/dev/null || true
    chown -R root:root /server/world/players 2>/dev/null || true
    chown -R root:root /server/world/level.dat 2>/dev/null || true
    if [ -d /server/plugins/FPixelCore/survival_data ]; then
        chown -R root:root /server/plugins/FPixelCore/survival_data 2>/dev/null || true
    fi
"

# 6. Clean up temporary directory
rm -rf "${TEMP_RESTORE_DIR}"

# 7. Start Minecraft server container
echo "Starting Minecraft server (${CONTAINER_NAME})..."
docker start "${CONTAINER_NAME}"

echo "=========================================="
echo " Survival World Restore Completed Successfully!"
echo " Server has been started."
echo "=========================================="
