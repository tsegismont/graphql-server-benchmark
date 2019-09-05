#!/bin/bash

set -e

LUA_FILE=$1
if [ ! -f "${LUA_FILE}" ]; then
  echo "${LUA_FILE} does not exist."
fi

CONNECTIONS=$2
DURATION=$3
REPORT_FILE=$4
SERVER_URL="http://${SERVER_HOST:-localhost}:8080/graphql"

export REPORT_FILE

wrk --latency -d $DURATION -c $CONNECTIONS -t 8 -s "${LUA_FILE}" ${SERVER_URL}
