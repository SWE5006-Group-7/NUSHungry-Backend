FROM alpine/java:17
COPY /target/*.jar /app.jar
COPY /uploads /uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]