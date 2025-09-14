FROM eclipse-temurin:21-jdk

WORKDIR /app

# Install gosu for proper user switching
RUN apt-get update && apt-get install -y gosu && rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy jar file first
COPY build/libs/user-rest-spring-service.jar app.jar

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Create logs directory with proper permissions and ownership
RUN mkdir -p logs && chmod 755 logs && chown -R appuser:appuser /app

# Ensure logs directory is writable by appuser
RUN chown -R appuser:appuser /app/logs && chmod -R 755 /app/logs

# Switch to non-root user
USER appuser

EXPOSE 8083

ENTRYPOINT ["/entrypoint.sh", "java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:default}", \
    "-Dmanagement.endpoints.web.exposure.include=*", \
    "-Dmanagement.endpoint.health.show-details=always", \
    "-Dmanagement.metrics.export.prometheus.enabled=true", \
    "-Dmanagement.metrics.distribution.percentiles-histogram.http.server.requests=true", \
    "-Dmanagement.metrics.distribution.percentiles.http.server.requests=0.5,0.9,0.95,0.99", \
    "-Dmanagement.metrics.distribution.slo.http.server.requests=50ms,100ms,200ms,500ms,1s,2s,5s", \
    "-jar", "/app/app.jar"]
