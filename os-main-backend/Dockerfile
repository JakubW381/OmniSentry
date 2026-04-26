FROM gradle:9.2-jdk21-alpine AS build
WORKDIR /app

COPY . .

RUN gradle :os-main-backend:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/os-main-backend/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]