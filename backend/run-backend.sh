#!/bin/bash

set -e

sudo docker build -t graphql-server-benchmark/backend .
sudo docker run --network host -it graphql-server-benchmark/backend
