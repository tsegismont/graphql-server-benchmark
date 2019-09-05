#!/bin/bash

set -e

trap "exit" INT

LUA_FILE=$1
if [ ! -f "${LUA_FILE}" ]; then
  echo "${LUA_FILE} does not exist."
fi

./warmup-server.sh "${LUA_FILE}"
./stress-server.sh "${LUA_FILE}" 8 60
./stress-server.sh "${LUA_FILE}" 16 60
./stress-server.sh "${LUA_FILE}" 32 60
./stress-server.sh "${LUA_FILE}" 64 60
./stress-server.sh "${LUA_FILE}" 128 60
./stress-server.sh "${LUA_FILE}" 256 60
