FROM maven:3-jdk-11-slim
WORKDIR /server
COPY src src
COPY pom.xml pom.xml
RUN mvn package -q
CMD ["java", "-Xms2G", "-Xmx2G", "-server", "-jar", "target/vertx-graphql-java", "-instances", "$((2 * $(grep --count ^processor /proc/cpuinfo)))", "-conf", "src/main/conf/server.json"]
