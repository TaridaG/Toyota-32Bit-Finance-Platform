FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY analytics-service/pom.xml analytics-service/pom.xml
COPY analytics-service/src analytics-service/src

RUN mvn -q -f pom.xml -N install -DskipTests \
    && mvn -q -f analytics-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/analytics-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
