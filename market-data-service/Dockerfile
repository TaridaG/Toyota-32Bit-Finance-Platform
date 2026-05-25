FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY market-data-service/pom.xml market-data-service/pom.xml
COPY market-data-service/src market-data-service/src

RUN mvn -q -f pom.xml -N install -DskipTests \
    && mvn -q -f market-data-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /workspace/market-data-service/target/market-data-service*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
