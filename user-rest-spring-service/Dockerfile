FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/user-rest-spring-service.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
