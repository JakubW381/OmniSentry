plugins {
    kotlin("jvm")
    id("io.ktor.plugin") version "3.4.3"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.google.protobuf")
}

group = "dev.jakubw.omnisentry"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("dev.jakubw.omnisentry.ApplicationKt")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":os-shared-core")){
        exclude(group = "org.springframework.boot")
        exclude(group = "org.springframework.grpc")
    }
    implementation(kotlin("reflect"))
    implementation("ai.koog:koog-agents:0.7.1")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")

    implementation("io.grpc:grpc-netty-shaded:1.59.0")
    implementation("io.grpc:grpc-protobuf:1.59.0")
    implementation("io.grpc:grpc-stub:1.59.0")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
    implementation("com.google.protobuf:protobuf-kotlin:3.24.0")
}


kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}