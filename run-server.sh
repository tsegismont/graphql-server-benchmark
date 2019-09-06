#!/bin/bash

set -e

trap "exit" INT

FRAMEWORK_DIR="$1"
if [ ! -d "${FRAMEWORK_DIR}" ]; then
  echo "\"${FRAMEWORK_DIR}\" is not a directory."
  exit 1
fi

FRAMEWORK_ENV_FILE="${FRAMEWORK_DIR}/set-env.sh"
if [ ! -f "${FRAMEWORK_ENV_FILE}" ]; then
  echo "\"${FRAMEWORK_ENV_FILE}\" file is missing."
  exit 1
fi

DOCKER_ENV=""
if [ -n "${BACKEND_HOST}" ]; then
  DOCKER_ENV="-e BACKEND_HOST=${BACKEND_HOST} ${DOCKER_ENV}"
fi
if [ -n "${POSTGRES_HOST}" ]; then
  DOCKER_ENV="-e POSTGRES_HOST=${POSTGRES_HOST} ${DOCKER_ENV}"
fi

cd "${FRAMEWORK_DIR}"
source set-env.sh
docker build -t graphql-server-benchmark/${FRAMEWORK_NAME} .
docker run --network host ${DOCKER_ENV} -it graphql-server-benchmark/${FRAMEWORK_NAME}
