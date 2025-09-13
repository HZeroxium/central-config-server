FROM eclipse-temurin:21-jre

WORKDIR /app

# Create logs directory with proper permissions
RUN mkdir -p logs && chmod 755 logs

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

COPY build/libs/user-rpc-thrift-service.jar app.jar

EXPOSE 8080 9090

ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
