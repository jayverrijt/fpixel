#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# FPixel Network - Full Repository & Server Daily GitHub Backup
# Automatically saves entire repository (code, plugins, web, configs) and all
# Minecraft server world data (dimensions, player profiles, custom realms)
# directly to GitHub branch 'daily_backups'.
# ==============================================================================

REPO_DIR="${REPO_DIR:-/home/jayv/fpixel}"
if [ ! -d "${REPO_DIR}" ] && [ -d "/Users/jayv/Projects/Fpixel2" ]; then
    REPO_DIR="/Users/jayv/Projects/Fpixel2"
fi

WORKTREE_DIR="${WORKTREE_DIR:-/tmp/fpixel_daily_backup_worktree}"
CONTAINER_NAME="${CONTAINER_NAME:-fpixel-minecraft}"
BRANCH_NAME="daily_backups"
REMOTE_NAME="origin"
SERVER_DIR="${SERVER_DIR:-${REPO_DIR}/server}"

echo "=========================================="
echo " Starting Full Repository Daily GitHub Backup"
echo " Timestamp: $(date +'%Y-%m-%d %H:%M:%S %Z')"
echo " Repository: ${REPO_DIR}"
echo " Target Branch: ${BRANCH_NAME}"
echo "=========================================="

# 1. Trigger save-all on Minecraft server via RCON or docker exec
if command -v docker >/dev/null 2>&1 && docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Saving worlds on Minecraft server (${CONTAINER_NAME})..."
    python3 - <<'EOF' || true
import socket
import struct

def rcon_command(host, port, password, cmd):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(4.0)
        s.connect((host, port))
        auth = password.encode('utf-8') + b'\x00\x00'
        s.sendall(struct.pack('<iii', len(auth) + 10, 1, 3) + auth)
        s.recv(4096)
        cmd_p = cmd.encode('utf-8') + b'\x00\x00'
        s.sendall(struct.pack('<iii', len(cmd_p) + 10, 2, 2) + cmd_p)
        s.recv(4096)
        s.close()
        print(f"RCON command '{cmd}' executed successfully.")
    except Exception as e:
        print(f"RCON save failed (non-fatal): {e}")

rcon_command("127.0.0.1", 25575, "fpixel_rcon_2026", "save-all")
EOF
    sleep 2
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

# 3. Copy full repository code and structure (excluding .git and temp)
echo "Archiving repository files (plugin, web, docker, scripts)..."
for item in plugin web docker scripts docker-compose.yml README.md .env.example; do
    if [ -e "${REPO_DIR}/${item}" ]; then
        cp -a "${REPO_DIR}/${item}" "${WORKTREE_DIR}/"
    fi
done

# Clean build artifacts inside worktree
rm -rf "${WORKTREE_DIR}/plugin/build" "${WORKTREE_DIR}/plugin/.gradle" "${WORKTREE_DIR}/web/node_modules" 2>/dev/null || true

# 4. Copy full server world data and configs
echo "Archiving server dimensions, player profiles, and plugin data..."
mkdir -p "${WORKTREE_DIR}/server"

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

# Copy server root configs
for f in server.properties bukkit.yml spigot.yml ops.json whitelist.json usercache.json; do
    if [ -f "${SERVER_DIR}/${f}" ]; then
        cp -f "${SERVER_DIR}/${f}" "${WORKTREE_DIR}/server/"
    fi
done

# Exclude unnecessary big logs and cache from server
rm -rf "${WORKTREE_DIR}/server/world/session.lock" "${WORKTREE_DIR}/server/plugins/FPixelCore/userdata/cache" 2>/dev/null || true

# 5. Write metadata file
TIMESTAMP="$(date +'%Y-%m-%d %H:%M:%S %Z')"
cat <<EOF > "${WORKTREE_DIR}/BACKUP_INFO.md"
# FPixel Daily Full Repository & Server Backup

- **Backup Timestamp:** ${TIMESTAMP}
- **Git Branch:** ${BRANCH_NAME}
- **Source Host:** $(hostname -f 2>/dev/null || hostname 2>/dev/null || echo "fpixel.nl")
- **Contents:**
  - Full codebase (\`plugin/\`, \`web/\`, \`docker/\`, \`scripts/\`)
  - All world dimensions & player data (\`server/world/\`)
  - Plugin data & storage (\`server/plugins/FPixelCore/\`)
  - Server configurations (\`server.properties\`, \`bukkit.yml\`, \`spigot.yml\`, \`ops.json\`)
EOF

# 6. Commit and Push
echo "Committing daily backup to git..."
git add -A
git commit -m "Daily Full Repository Backup: ${TIMESTAMP}" || echo "No changes to commit."

echo "Pushing daily backup directly to GitHub (${REMOTE_NAME}/${BRANCH_NAME})..."
git push -u "${REMOTE_NAME}" "${BRANCH_NAME}" -f

# 7. Cleanup worktree
echo "Cleaning up local temporary worktree..."
cd "${REPO_DIR}"
git worktree remove --force "${WORKTREE_DIR}" || rm -rf "${WORKTREE_DIR}"
git worktree prune 2>/dev/null || true

echo "=========================================="
echo " Daily Full Repository Backup Completed Successfully!"
echo " Pushed to: ${REMOTE_NAME}/${BRANCH_NAME}"
echo " Zero persistent local backup files stored on disk."
echo "=========================================="
