FROM gradle:9.2-jdk21 AS build
WORKDIR /app

COPY . .

RUN gradle :os-authenticator:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/os-authenticator/build/libs/*.jar app.jar
EXPOSE 8081
EXPOSE 9091
ENTRYPOINT ["java", "-jar", "app.jar"]