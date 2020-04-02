FROM maven:3-jdk-11-slim
WORKDIR /server
COPY src src
COPY pom.xml pom.xml
RUN mvn package -q
CMD ["java", "-Xms2G", "-Xmx2G", "-server", "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory", "-jar", "target/vertx-graphql-java", "-instances", "8", "-conf", "src/main/conf/server.json"]
