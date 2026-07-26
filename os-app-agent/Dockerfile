FROM gradle:8.5-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

RUN ./gradlew :os-app-agent:shadowJar --no-daemon

# Runtime
FROM eclipse-temurin:21-jre-jammy
EXPOSE 8085

WORKDIR /app

COPY --from=build /home/gradle/src/os-app-agent/build/libs/*.jar app.jar

ENV GRPC_HOST="os-analyser"
ENV GRPC_PORT="9093"

ENTRYPOINT ["java", "-jar", "app.jar", "basic"]