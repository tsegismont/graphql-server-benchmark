#!/bin/bash

set -e

LUA_FILE=$1
if [ ! -f "${LUA_FILE}" ]; then
  echo "${LUA_FILE} does not exist."
fi

SERVER_URL="http://${SERVER_HOST:-localhost}:8080/graphql"

wrk --latency -d 60 -c 256 -t 8 -s "${LUA_FILE}" ${SERVER_URL} >/dev/null
wrk --latency -d 60 -c 256 -t 8 -s "${LUA_FILE}" ${SERVER_URL} >/dev/null
