FROM maven:3-jdk-11-slim
WORKDIR /backend
COPY src src
COPY pom.xml pom.xml
RUN mvn package -q
CMD ["java", "-Xms2G", "-Xmx2G", "-server", "-jar", "target/backend-1.0-SNAPSHOT.jar", "-instances", "8", "-conf", "src/main/conf/backend.json"]