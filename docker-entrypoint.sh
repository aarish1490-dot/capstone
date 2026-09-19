#!/usr/bin/env bash
set -e

# DhatchinaMart container entrypoint.
#
# Handles two platform concerns so the same image works on Render, Fly, Dokku
# or a plain `docker run`:
#
#  1. PORT - hosts assign a random port (Render default 10000). Rewrite Tomcat's
#     HTTP connector to listen on $PORT (default 8080) unless one is assigned.
#  2. Persistent database - when a disk is mounted, Render exports RENDER_DISK_PATH
#     and creates the directory. Point H2 there so data survives redeploys.
#     If DHAT_DB_URL is explicitly configured it always wins.

# Persistent H2 on the platform disk (Render only).
if [ -z "${DHAT_DB_URL}" ] && [ -n "${RENDER_DISK_PATH}" ]; then
  export DHAT_DB_URL="jdbc:h2:file:${RENDER_DISK_PATH}/dhatchinamart;AUTO_SERVER=TRUE"
  echo "DHAT_DB_URL not set - using persistent H2 on RENDER_DISK_PATH=${RENDER_DISK_PATH}"
fi

# Platform-assigned HTTP port.
PORT="${PORT:-8080}"
if [ -n "${CATALINA_HOME}" ] && [ -f "${CATALINA_HOME}/conf/server.xml" ]; then
  sed -i "s/port=\"8080\"/port=\"${PORT}\"/" "${CATALINA_HOME}/conf/server.xml"
  echo "Tomcat HTTP connector bound to port ${PORT}"
fi

exec catalina.sh run