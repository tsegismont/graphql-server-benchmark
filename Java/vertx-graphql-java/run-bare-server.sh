#!/bin/bash

set -e

trap "exit" INT

mvn clean package
java -Xms2G -Xmx2G -server -jar target/vertx-graphql-java.jar -instances $((2 * $(grep --count ^processor /proc/cpuinfo))) -conf src/main/conf/server.json
