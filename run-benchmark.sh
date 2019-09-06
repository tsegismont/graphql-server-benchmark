#!/bin/bash

set -e

trap "exit" INT

FRAMEWORK_DIR="$1"
if [ ! -d "${FRAMEWORK_DIR}" ]; then
  echo "\"${FRAMEWORK_DIR}\" is not a directory."
  exit 1
fi

LUA_FILE=$2
if [ ! -f "${LUA_FILE}" ]; then
  echo "\"${LUA_FILE}\" does not exist."
fi

REPORT_FILE="${FRAMEWORK_DIR}/${LUA_FILE}.report"
rm -f "${REPORT_FILE}"

DURATION="1m"

echo "Warming up framework ${FRAMEWORK_DIR} for benchmark ${LUA_FILE}..."
./warmup-server.sh "${LUA_FILE}"

for connections in 8 16 32 64 128 256; do
  echo "Running benchmark ${LUA_FILE} for framework ${FRAMEWORK_DIR} with $connections connections..."
  ./stress-server.sh "${LUA_FILE}" $connections $DURATION "$REPORT_FILE"
done
