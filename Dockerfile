FROM openjdk:11-jre-slim

COPY target/scala-2.12/server.jar /

RUN ls /

ENTRYPOINT ["java", "-jar", "/server.jar"]