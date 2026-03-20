FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 7777

ENTRYPOINT ["java", "-jar", "app.jar"]