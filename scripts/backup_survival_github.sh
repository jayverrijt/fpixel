#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# FPixel Network - Survival & Full Repository Daily GitHub Backup
# Redirects to daily_backup.sh for full repository and server backup on 'daily_backups'
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "${SCRIPT_DIR}/daily_backup.sh" "$@"
