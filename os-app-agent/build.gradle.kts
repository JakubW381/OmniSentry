import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("io.ktor.plugin") version "3.4.3"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.protobuf")
    id("com.gradleup.shadow") version "8.3.5"
}

group = "dev.jakubw.omnisentry"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("dev.jakubw.omnisentry.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(project(":os-shared-core")) {
        exclude(group = "org.springframework.boot")
        exclude(group = "org.springframework.grpc")
    }

    implementation(kotlin("reflect"))
    implementation("ai.koog:koog-agents:0.7.1")

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")

    val grpcVersion = "1.60.0"
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-services:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("com.google.protobuf:protobuf-kotlin:3.24.0")

    implementation(platform("io.insert-koin:koin-bom:4.1.1"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-ktor")

    implementation("ch.qos.logback:logback-classic:1.5.12")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()

    archiveFileName.set("app.jar")

    manifest {
        attributes["Main-Class"] = "dev.jakubw.omnisentry.ApplicationKt"
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}