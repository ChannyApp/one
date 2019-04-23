FROM hseeberger/scala-sbt:latest
COPY . .
RUN sbt assembly && mv target/scala-2.12/server.jar /

FROM openjdk:11-jre-slim
COPY --from=0 /server.jar /
ENTRYPOINT ["java", "-jar", "/server.jar"]