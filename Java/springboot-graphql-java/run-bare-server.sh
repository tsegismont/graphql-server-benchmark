#!/bin/bash

set -e

trap "exit" INT

mvn clean package
java -Xms2G -Xmx2G -server -jar target/springboot-graphql-java.jar
