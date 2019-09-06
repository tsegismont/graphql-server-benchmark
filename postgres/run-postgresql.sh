#!/bin/bash

set -e

trap "exit" INT

docker build -t graphql-server-benchmark/postgres .
docker run --network host -it graphql-server-benchmark/postgres \
  -c "listen_addresses=*" \
  -c "max_connections=2000" \
  -c "shared_buffers=256MB" \
  -c "work_mem=64MB" \
  -c "maintenance_work_mem=512MB" \
  -c "checkpoint_completion_target=0.9" \
  -c "effective_cache_size=8GB" \
  -c "random_page_cost=2"
