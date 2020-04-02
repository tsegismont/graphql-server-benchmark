#!/bin/bash

set -e

trap "exit" INT

mvn clean package
java -Xms2G -Xmx2G -server -jar target/vertx-graphql-java.jar -instances 8 -conf src/main/conf/server.json
