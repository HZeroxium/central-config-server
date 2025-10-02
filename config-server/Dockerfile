FROM eclipse-temurin:21-jre

ARG APP_JAR=build/libs/config-server-1.0.0.jar

WORKDIR /app

COPY ${APP_JAR} app.jar

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

EXPOSE 8888

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

