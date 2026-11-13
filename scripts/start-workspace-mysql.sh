#!/bin/sh
set -eu
PROJECT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TASK=$(CDPATH= cd -- "$PROJECT/../.." && pwd)
MYSQL_HOME="$TASK/work/tools/mysql-8.4.4-macos15-arm64"
DATA="$TASK/work/mysql-standalone"
mkdir -p "$DATA"
if [ ! -d "$DATA/mysql" ]; then
 "$MYSQL_HOME/bin/mysqld" --initialize-insecure --datadir="$DATA"
fi
exec "$MYSQL_HOME/bin/mysqld" --datadir="$DATA" --bind-address=127.0.0.1 --port=33306 --socket="/tmp/iitsm-33306.sock" --mysqlx=OFF --pid-file="$DATA/server.pid" --log-error="$DATA/server.log"
