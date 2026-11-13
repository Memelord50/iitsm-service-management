#!/bin/sh
set -eu
PROJECT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT"
if [ -f .env ]; then set -a; . ./.env; set +a; fi
: "${TEST_DB_URL:?Set TEST_DB_URL to a dedicated empty MySQL test database}"
export TEST_DB_USER="${TEST_DB_USER:-$DB_USER}"
export TEST_DB_PASSWORD="${TEST_DB_PASSWORD:-$DB_PASSWORD}"
cd backend
exec mvn test
