#!/usr/bin/env bash
set -euo pipefail

### CONFIG ###
SSH_USER="root"                     # <--- change me
HOST="10.40.30.233"                 # <--- change me
REMOTE_DIR="/root/huyng5_fresher/ztf-projects/demo-project"

# Only these paths (relative to current dir) will be synced.
# You can add/remove lines.
INCLUDE_LIST=(
  "docker-compose.yml"
  "docker-compose.min.yml"
  "config"
#   "docker_images"  # keep if you also want to move the tar files/folder structure
)

# SSH/rsync robustness
RSYNC_SSH_OPTS="-o ServerAliveInterval=30 -o ServerAliveCountMax=6"
# --delete to mirror removals; remove it if you don't want deletions on remote.
RSYNC_OPTS="-avz -r --delete --partial --inplace --progress"

MAX_RETRY=3
RETRY_DELAY=10

### FUNCTIONS ###
die(){ echo "[ERROR] $*" >&2; exit 1; }

### PREP LIST FILE FOR --files-from ###
# rsync --files-from expects relative paths from source (here: "./")
# If a path is a directory, rsync will recurse due to -a (archive implies -r). 
LIST=$(mktemp)
for p in "${INCLUDE_LIST[@]}"; do
  printf "%s\n" "$p" >> "$LIST"
done

echo "=== Ensuring remote dir exists ==="
ssh ${SSH_USER}@${HOST} "mkdir -p '${REMOTE_DIR}'"

echo "=== Sync only listed items (files-from) ==="
# This copies *only* entries from LIST; nothing else.
# Docs and examples for --files-from: see refs. 
# Source is "./" so listed paths are relative to current directory.
attempt=1
while (( attempt <= MAX_RETRY )); do
  if rsync ${RSYNC_OPTS} -e "ssh ${RSYNC_SSH_OPTS}" \
       --files-from="$LIST" \
       ./ "${SSH_USER}@${HOST}:${REMOTE_DIR}/"; then
    break
  fi
  echo "[WARN] rsync attempt #$attempt failed; retry in ${RETRY_DELAY}s..."
  attempt=$((attempt+1))
  sleep "${RETRY_DELAY}"
done

rm -f "$LIST"
(( attempt > MAX_RETRY )) && die "rsync failed after ${MAX_RETRY} attempts"

echo "=== Done (sync_selected.sh) ==="
