FROM gradle:9.2-jdk21 AS build
WORKDIR /app

COPY . .

RUN gradle :os-main-backend:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends \
    bash \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/os-main-backend/build/libs/*.jar app.jar
EXPOSE 8082
EXPOSE 9092
ENTRYPOINT ["java", "-jar", "app.jar"]