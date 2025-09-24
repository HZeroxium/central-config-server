#!/usr/bin/env bash
set -euo pipefail

### CONFIG ###
SSH_USER="root"                     # <--- change me
HOST="10.40.30.233"                 # <--- change me
REMOTE_DIR="/root/huyng5_fresher/ztf-projects/demo-project"

# Images to transfer (repo[:tag]). You can add/remove as needed.
IMAGE_LIST=(
  "hzeroxium/user-rest-spring-service"
#   "hzeroxium/user-thrift-server-service"
#   "hzeroxium/user-watcher-service"
#   "bitnami/kafka:3.9"
#   "redis:latest"
#   "mongo:8.0"
)

LOCAL_IMAGE_DIR="./docker_images"

# SSH/rsync robustness
RSYNC_SSH_OPTS="-o ServerAliveInterval=30 -o ServerAliveCountMax=6"
RSYNC_OPTS="-avz --partial --inplace --progress"

### FUNCTIONS ###
die(){ echo "[ERROR] $*" >&2; exit 1; }

### SAVE IMAGES LOCALLY ###
mkdir -p "${LOCAL_IMAGE_DIR}"

echo "=== Saving docker images to ${LOCAL_IMAGE_DIR} ==="
for img in "${IMAGE_LIST[@]}"; do
  safe=$(echo "${img}" | tr '/:' '__')
  out="${LOCAL_IMAGE_DIR}/${safe}.tar"
  echo "Saving '${img}' -> '${out}'"
  docker save "${img}" -o "${out}" || die "docker save failed for ${img}"  # docker save doc: https://docs.docker.com/reference/cli/docker/image/save/
done

### PUSH ONLY docker_images/*.tar TO REMOTE ###
echo "=== Ensuring remote dir exists ==="
ssh ${SSH_USER}@${HOST} "mkdir -p '${REMOTE_DIR}/docker_images'"

echo "=== Rsync images (*.tar) ==="
rsync ${RSYNC_OPTS} -e "ssh ${RSYNC_SSH_OPTS}" \
  "${LOCAL_IMAGE_DIR}/" \
  "${SSH_USER}@${HOST}:${REMOTE_DIR}/docker_images/"

### LOAD IMAGES ON REMOTE (scan files, don't rely on local arrays) ###
echo "=== Remote: loading images from docker_images/*.tar ==="
ssh ${SSH_USER}@${HOST} bash -lc "
  set -euo pipefail
  cd '${REMOTE_DIR}'
  shopt -s nullglob
  files=(docker_images/*.tar)
  if (( \${#files[@]} == 0 )); then
    echo '[WARN] No .tar files found under docker_images/. Nothing to load.'
    exit 0
  fi
  for f in \"\${files[@]}\"; do
    echo \"Loading image from '\$f'...\"
    docker load -i \"\$f\"   # docker load doc: https://docs.docker.com/reference/cli/docker/image/load/
  done
  echo '[OK] All images loaded.'
"

echo "=== Done (push_images.sh) ==="
