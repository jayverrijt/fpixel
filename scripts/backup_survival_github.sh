#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# FPixel Network - Survival World Daily GitHub Backup
# Automatically saves Minecraft survival world data and pushes directly to GitHub
# on branch 'backups_survival'. No persistent backup files are stored locally.
# ==============================================================================

REPO_DIR="${REPO_DIR:-/home/jayv/fpixel}"
WORKTREE_DIR="${WORKTREE_DIR:-/tmp/fpixel_survival_backup_worktree}"
CONTAINER_NAME="${CONTAINER_NAME:-fpixel-minecraft}"
BRANCH_NAME="backups_survival"
REMOTE_NAME="origin"

echo "=========================================="
echo " Starting Survival World GitHub Backup"
echo " Timestamp: $(date +'%Y-%m-%d %H:%M:%S %Z')"
echo " Repository: ${REPO_DIR}"
echo " Target Branch: ${BRANCH_NAME}"
echo "=========================================="

# 1. Trigger save-all on Minecraft server via docker exec
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Saving world on Minecraft server (${CONTAINER_NAME})..."
    # Send save-all command via RCON using Python
    python3 - <<'EOF' || true
import socket
import struct
import time

def rcon_command(host, port, password, cmd):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(4.0)
        s.connect((host, port))
        
        # Login (type 3)
        req_id = 1
        auth_payload = password.encode('utf-8') + b'\x00\x00'
        packet = struct.pack('<iii', len(auth_payload) + 10, req_id, 3) + auth_payload
        s.sendall(packet)
        resp = s.recv(4096)
        
        # Execute command (type 2)
        req_id = 2
        cmd_payload = cmd.encode('utf-8') + b'\x00\x00'
        packet = struct.pack('<iii', len(cmd_payload) + 10, req_id, 2) + cmd_payload
        s.sendall(packet)
        resp = s.recv(4096)
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
git worktree prune

echo "Setting up isolated worktree for branch ${BRANCH_NAME}..."

# Check if branch exists on remote or locally
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

# Clean previous contents in worktree
cd "${WORKTREE_DIR}"
find . -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +

# 3. Export survival data directly from container to worktree
echo "Copying survival world files from container..."
mkdir -p "${WORKTREE_DIR}/dimensions/minecraft"
mkdir -p "${WORKTREE_DIR}/world"
mkdir -p "${WORKTREE_DIR}/plugins/FPixelCore"

# Copy survival world dimension
docker cp "${CONTAINER_NAME}:/data/world/dimensions/minecraft/survival" "${WORKTREE_DIR}/dimensions/minecraft/survival"

# Copy player data, advancements, and stats
docker cp "${CONTAINER_NAME}:/data/world/players" "${WORKTREE_DIR}/world/players"

# Copy world level.dat
docker cp "${CONTAINER_NAME}:/data/world/level.dat" "${WORKTREE_DIR}/world/level.dat" 2>/dev/null || true

# Copy FPixelCore survival data (inventories, bank accounts, homes, claims)
if docker exec "${CONTAINER_NAME}" test -d /data/plugins/FPixelCore/survival_data; then
    docker cp "${CONTAINER_NAME}:/data/plugins/FPixelCore/survival_data" "${WORKTREE_DIR}/plugins/FPixelCore/survival_data"
fi

# 4. Generate metadata file
TIMESTAMP="$(date +'%Y-%m-%d %H:%M:%S %Z')"
cat <<EOF > "${WORKTREE_DIR}/BACKUP_INFO.md"
# FPixel Survival World Daily Backup

- **Backup Timestamp:** ${TIMESTAMP}
- **Git Branch:** ${BRANCH_NAME}
- **Source Host:** $(hostname -f 2>/dev/null || hostname)
- **World Dimension:** \`dimensions/minecraft/survival\`
- **Player Profiles:** \`world/players\`
- **Plugin Storage:** \`plugins/FPixelCore/survival_data\`
- **Level Metadata:** \`world/level.dat\`

### Restore Instructions:
1. Stop the Minecraft server: \`docker stop fpixel-minecraft\`
2. Copy contents from this branch back into \`server/\`:
   - \`dimensions/minecraft/survival\` -> \`server/world/dimensions/minecraft/survival\`
   - \`world/players\` -> \`server/world/players\`
   - \`world/level.dat\` -> \`server/world/level.dat\`
   - \`plugins/FPixelCore/survival_data\` -> \`server/plugins/FPixelCore/survival_data\`
3. Start the Minecraft server: \`docker start fpixel-minecraft\`
EOF

# 5. Git Commit and Push to GitHub
echo "Committing survival backup to git..."
git add -A
git commit -m "Survival Daily Backup: ${TIMESTAMP}" || echo "No changes to commit."

echo "Pushing backup directly to GitHub (${REMOTE_NAME}/${BRANCH_NAME})..."
git push -u "${REMOTE_NAME}" "${BRANCH_NAME}"

# 6. Complete cleanup - NEVER retain local backup files!
echo "Cleaning up local temporary worktree..."
cd "${REPO_DIR}"
git worktree remove --force "${WORKTREE_DIR}" || rm -rf "${WORKTREE_DIR}"
git worktree prune

echo "=========================================="
echo " Survival World GitHub Backup Completed Successfully!"
echo " Pushed to: ${REMOTE_NAME}/${BRANCH_NAME}"
echo " Zero persistent local backup files stored on disk."
echo "=========================================="
