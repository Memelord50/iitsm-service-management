#!/bin/sh
set -eu
PROJECT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT"
if [ -f .env ]; then set -a; . ./.env; set +a; fi
cd backend
exec mvn spring-boot:run
