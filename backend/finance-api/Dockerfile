FROM eclipse-temurin:21-jre

WORKDIR /app

# Eski hali buydu:
# COPY target/*.jar app.jar

# Yeni hali bu olmalı (Yolu tam gösteriyorum):
COPY backend/finance-api/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
