#!/bin/sh
set -eu
PROJECT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$PROJECT"
if [ -f .env ]; then set -a; . ./.env; set +a; fi
LOCAL_JDK="$PROJECT/../../work/tools/jdk-25.0.5.0.3.jdk/Contents/Home"
if [ -n "${JAVA_HOME:-}" ]; then JAVA_BIN="$JAVA_HOME/bin/java"
elif [ -x "$LOCAL_JDK/bin/java" ]; then JAVA_BIN="$LOCAL_JDK/bin/java"
else JAVA_BIN=java
fi
exec "$JAVA_BIN" -jar backend/target/i-itsm-1.0.0.jar
