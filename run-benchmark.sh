#!/bin/bash

set -e

trap "exit" INT

FRAMEWORK_DIR="$1"
if [ ! -d "${FRAMEWORK_DIR}" ]; then
  echo "${FRAMEWORK_DIR} is not a directory."
  exit 1
fi

LUA_FILE=$2
if [ ! -f "${LUA_FILE}" ]; then
  echo "${LUA_FILE} does not exist."
fi

REPORT_FILE="${FRAMEWORK_DIR}/${LUA_FILE}.report"
rm -f "${REPORT_FILE}"

./warmup-server.sh "${LUA_FILE}"
./stress-server.sh "${LUA_FILE}" 8 60 "$REPORT_FILE"
./stress-server.sh "${LUA_FILE}" 16 60 "$REPORT_FILE"
./stress-server.sh "${LUA_FILE}" 32 60 "$REPORT_FILE"
./stress-server.sh "${LUA_FILE}" 64 60 "$REPORT_FILE"
./stress-server.sh "${LUA_FILE}" 128 60 "$REPORT_FILE"
./stress-server.sh "${LUA_FILE}" 256 60 "$REPORT_FILE"
