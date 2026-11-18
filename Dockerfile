FROM amazoncorretto:21-alpine
WORKDIR /app
COPY target/questionnaire-service-0.1.0.jar app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "app.jar"]
