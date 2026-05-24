FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY platform-logging platform-logging
COPY news-service/pom.xml news-service/pom.xml
COPY news-service/src news-service/src

RUN mvn -q -f pom.xml -N install -DskipTests \
    && mvn -q -f platform-logging/pom.xml install -DskipTests \
    && mvn -q -f news-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && groupadd --system appgroup \
    && useradd --system --gid appgroup --create-home --home-dir /home/appuser appuser \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/news-service/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
