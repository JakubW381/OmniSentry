FROM gradle:9.2-jdk21-alpine AS build
WORKDIR /app

COPY . .

RUN gradle :os-authenticator:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/os-authenticator/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]