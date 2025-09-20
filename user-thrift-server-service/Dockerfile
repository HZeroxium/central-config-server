FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY build/libs/user-thrift-server-service.jar app.jar

EXPOSE 9090 8085

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]


