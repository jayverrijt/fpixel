#!/usr/bin/env bash
set -euo pipefail

# Configuration with environment overrides
BASE_DIR="${BASE_DIR:-/home/jayv/fpixel}"
SERVER_DIR="${SERVER_DIR:-$BASE_DIR/server}"
BACKUP_DIR="${BACKUP_DIR:-/home/jayv/backups}"
MAX_BACKUPS="${MAX_BACKUPS:-15}"

TIMESTAMP="$(date +'%Y%m%d_%H%M%S')"
BACKUP_FILE="${BACKUP_DIR}/fpixel_backup_${TIMESTAMP}.tar.gz"

echo "=========================================="
echo " Starting FPixel Automated Backup"
echo " Timestamp: ${TIMESTAMP}"
echo " Source:    ${SERVER_DIR}"
echo " Target:    ${BACKUP_FILE}"
echo "=========================================="

mkdir -p "${BACKUP_DIR}"

if [ ! -d "${SERVER_DIR}" ]; then
  # Fallback if server directory is directly at BASE_DIR
  if [ -d "${BASE_DIR}/survival" ]; then
    SERVER_DIR="${BASE_DIR}/survival"
  else
    SERVER_DIR="${BASE_DIR}"
  fi
  echo "Adjusted SERVER_DIR to: ${SERVER_DIR}"
fi

# Create compressed backup excluding caches, logs, libraries, and tmp files
# GNU tar exits with 1 if files change while archiving a live server; treat as non-fatal warning
tar --exclude='logs/*' \
    --exclude='cache/*' \
    --exclude='libraries/*' \
    --exclude='.paper/*' \
    --exclude='plugins/.paper-remapped/*' \
    --exclude='plugins/spark/tmp/*' \
    --exclude='plugins/spark/tmp-client/*' \
    --exclude='world_safety_snapshots/*' \
    --exclude='*.tmp' \
    --exclude='.git' \
    -czf "${BACKUP_FILE}" \
    -C "$(dirname "${SERVER_DIR}")" \
    "$(basename "${SERVER_DIR}")" || true

BACKUP_SIZE="$(du -h "${BACKUP_FILE}" | cut -f1)"
echo " Backup created successfully: ${BACKUP_FILE} (${BACKUP_SIZE})"

# Retention Policy: keep last $MAX_BACKUPS backups
echo "Applying retention policy (keeping newest ${MAX_BACKUPS} backups)..."
PRUNED_COUNT=0
for old_backup in $(ls -1dt "${BACKUP_DIR}"/fpixel_backup_*.tar.gz 2>/dev/null | tail -n +$((MAX_BACKUPS + 1))); do
  echo "Pruning old backup: ${old_backup}"
  rm -f "${old_backup}"
  PRUNED_COUNT=$((PRUNED_COUNT + 1))
done

TOTAL_BACKUPS="$(ls -1 "${BACKUP_DIR}"/fpixel_backup_*.tar.gz 2>/dev/null | wc -l | tr -d ' ')"
echo "Retention complete: pruned ${PRUNED_COUNT} old backup(s). Total retained: ${TOTAL_BACKUPS}."
echo "=========================================="
