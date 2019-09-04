#!/bin/bash

set -e

docker build -t graphql-server-benchmark/backend .
docker run --network host -it graphql-server-benchmark/backend
