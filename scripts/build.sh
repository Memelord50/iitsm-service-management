#!/bin/sh
set -eu
PROJECT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT/frontend"
npm ci
npm test
npm run build
cd "$PROJECT/backend"
mvn clean package
