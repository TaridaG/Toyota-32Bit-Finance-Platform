FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY log-consumer-service/pom.xml log-consumer-service/pom.xml
COPY log-consumer-service/src log-consumer-service/src

RUN mvn -q -f log-consumer-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/log-consumer-service/target/*.jar app.jar

EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
