FROM eclipse-temurin:21-jdk

WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs

# Copy jar file
COPY build/libs/user-watcher-service.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-default}", \
  "-Dmanagement.endpoints.web.exposure.include=*", \
  "-Dmanagement.endpoint.health.show-details=always", \
  "-Dmanagement.metrics.export.prometheus.enabled=true", \
  "-jar", "/app/app.jar"]


