FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.2.0 as builder

WORKDIR /app
COPY build.sbt ./
COPY project ./project
RUN sbt update
COPY src ./src
RUN sbt compile stage

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/target/universal/stage ./

RUN chmod +x bin/os-analyser

EXPOSE 9093

ENTRYPOINT ["bin/os-analyser"]