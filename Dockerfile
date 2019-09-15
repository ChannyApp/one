FROM hseeberger/scala-sbt:11.0.3_1.2.8_2.13.0
COPY . .
RUN sbt assembly && mv target/scala-2.13/server.jar /

FROM openjdk:13-alpine
COPY --from=0 /server.jar /
ENTRYPOINT ["java", "-jar", "/server.jar"]
