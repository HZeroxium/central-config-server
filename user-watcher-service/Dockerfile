FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY build/libs/user-watcher-service.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]


