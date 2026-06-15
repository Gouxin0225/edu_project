#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_DIR="${DEPLOY_DIR}/backup"

cd "${DEPLOY_DIR}"
set -a
source .env
set +a

mkdir -p "${BACKUP_DIR}"
docker exec edu-platform-mysql mariadb-dump -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" > "${BACKUP_DIR}/${MYSQL_DATABASE}-$(date +%Y%m%d%H%M%S).sql"

echo "Database backup saved to ${BACKUP_DIR}"
