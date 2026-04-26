FROM gradle:9.2-jdk21-alpine AS build
WORKDIR /app

COPY . .

RUN gradle :os-gateway:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/os-gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]