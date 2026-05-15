plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.plugin.jpa")
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

    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter:1.0.3")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-tomcat")

    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("org.projectlombok:lombok:1.18.42")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
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

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}