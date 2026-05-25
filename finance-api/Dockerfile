FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY finance-api/pom.xml finance-api/pom.xml
COPY finance-api/src finance-api/src

RUN mvn -q -f finance-api/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/finance-api/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
