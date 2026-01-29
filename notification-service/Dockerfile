FROM eclipse-temurin:21-jre

WORKDIR /app

# Burayı tam yol olacak şekilde değiştiriyorum:
COPY notification-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]