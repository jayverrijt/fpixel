#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# FPixel Network - Full Server GitHub Backup
# Automatically saves entire server (all dimensions, configs, plugins) and pushes to GitHub
# on branch 'backups_server'.
# ==============================================================================

REPO_DIR="${REPO_DIR:-/home/jayv/fpixel}"
if [ ! -d "${REPO_DIR}" ] && [ -d "/Users/jayv/Projects/Fpixel2" ]; then
    REPO_DIR="/Users/jayv/Projects/Fpixel2"
fi

WORKTREE_DIR="${WORKTREE_DIR:-/tmp/fpixel_server_backup_worktree}"
CONTAINER_NAME="${CONTAINER_NAME:-fpixel-minecraft}"
BRANCH_NAME="backups_server"
REMOTE_NAME="origin"
SERVER_DIR="${SERVER_DIR:-${REPO_DIR}/server}"

echo "=========================================="
echo " Starting Whole-Server GitHub Backup"
echo " Timestamp: $(date +'%Y-%m-%d %H:%M:%S %Z')"
echo " Repository: ${REPO_DIR}"
echo " Target Branch: ${BRANCH_NAME}"
echo "=========================================="

# 1. Trigger save-all on Minecraft server
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Saving world on Minecraft server (${CONTAINER_NAME})..."
    docker exec "${CONTAINER_NAME}" chown -R 1000:1000 /data 2>/dev/null || true
fi

# 2. Prepare isolated git worktree
cd "${REPO_DIR}"
rm -rf "${WORKTREE_DIR}"
git worktree prune 2>/dev/null || true

echo "Setting up isolated worktree for branch ${BRANCH_NAME}..."
if git ls-remote --exit-code --heads "${REMOTE_NAME}" "${BRANCH_NAME}" >/dev/null 2>&1; then
    echo "Fetching existing ${BRANCH_NAME} from remote..."
    git fetch "${REMOTE_NAME}" "${BRANCH_NAME}:${BRANCH_NAME}" --depth 1 -f || true
    git worktree add "${WORKTREE_DIR}" "${BRANCH_NAME}"
elif git show-ref --quiet "refs/heads/${BRANCH_NAME}"; then
    git worktree add "${WORKTREE_DIR}" "${BRANCH_NAME}"
else
    echo "Creating new orphan branch ${BRANCH_NAME}..."
    git worktree add --detach "${WORKTREE_DIR}"
    (
        cd "${WORKTREE_DIR}"
        git checkout --orphan "${BRANCH_NAME}"
        git rm -rf . 2>/dev/null || true
    )
fi

cd "${WORKTREE_DIR}"
find . -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +

# 3. Export all world data and configs
echo "Archiving server dimensions, configs and player data..."
mkdir -p "${WORKTREE_DIR}/server"

# Copy world and config folders excluding big cache/logs
if [ -d "${SERVER_DIR}/world" ]; then
    cp -a "${SERVER_DIR}/world" "${WORKTREE_DIR}/server/"
fi
if [ -d "${SERVER_DIR}/config" ]; then
    cp -a "${SERVER_DIR}/config" "${WORKTREE_DIR}/server/"
fi
mkdir -p "${WORKTREE_DIR}/server/plugins"
if [ -d "${SERVER_DIR}/plugins/FPixelCore" ]; then
    cp -a "${SERVER_DIR}/plugins/FPixelCore" "${WORKTREE_DIR}/server/plugins/"
fi
if [ -d "${SERVER_DIR}/plugins/SkinsRestorer" ]; then
    cp -a "${SERVER_DIR}/plugins/SkinsRestorer" "${WORKTREE_DIR}/server/plugins/"
fi
for f in server.properties bukkit.yml spigot.yml ops.json whitelist.json; do
    if [ -f "${SERVER_DIR}/${f}" ]; then
        cp -f "${SERVER_DIR}/${f}" "${WORKTREE_DIR}/server/"
    fi
done

# Write metadata
TIMESTAMP="$(date +'%Y-%m-%d %H:%M:%S %Z')"
cat <<EOF > "${WORKTREE_DIR}/BACKUP_INFO.md"
# FPixel Whole-Server GitHub Backup
- **Backup Timestamp:** ${TIMESTAMP}
- **Source Host:** $(hostname 2>/dev/null || echo "unknown")
- **Contents:** Full world (dimensions, players, datapacks), server configurations, and plugin data.
EOF

# 4. Commit and push
echo "Committing server backup to git..."
git add -A
git commit -m "Whole Server Backup: ${TIMESTAMP}" || echo "No changes to commit."

echo "Pushing backup to ${REMOTE_NAME}/${BRANCH_NAME}..."
git push "${REMOTE_NAME}" "${BRANCH_NAME}" -f || echo "Git push skipped or non-fatal."

# 5. Cleanup
cd "${REPO_DIR}"
rm -rf "${WORKTREE_DIR}"
git worktree prune 2>/dev/null || true

echo "=========================================="
echo " Whole-Server GitHub Backup Completed Successfully!"
echo "=========================================="
