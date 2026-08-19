FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S appgroup \
    && adduser -S appuser -G appgroup

COPY target/sanitation-assessment-0.0.1-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]