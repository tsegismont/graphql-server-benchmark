#!/bin/bash

set -e

trap "exit" INT

docker build -t graphql-server-benchmark/backend .
docker run --network host -it graphql-server-benchmark/backend
