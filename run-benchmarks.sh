#!/bin/bash

set -e

trap "exit" INT

FRAMEWORK_DIR="$1"
if [ ! -d "${FRAMEWORK_DIR}" ]; then
  echo "\"${FRAMEWORK_DIR}\" is not a directory."
  exit 1
fi

for script in *.lua; do
  if [ "${script}" != "report.lua" ]; then
    ./run-benchmark.sh "${FRAMEWORK_DIR}" "${script}"
  fi
done
