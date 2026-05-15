plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.google.protobuf")
}

group = "dev.jakubw.omnisentry"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation(platform("io.grpc:grpc-bom:1.70.0"))

    implementation(project(":os-shared-core"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter:1.0.3")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "io.grpc") {
                useVersion("1.70.0")
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}