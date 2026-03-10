FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/analytics-service.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]