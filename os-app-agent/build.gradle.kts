import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-3.0:2.29.0-alpha")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.64.0")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.64.0")
    implementation("io.opentelemetry.instrumentation:opentelemetry-grpc-1.6:2.29.0-alpha")

    implementation("io.netty:netty-transport-native-epoll:4.1.115.Final:linux-x86_64")
    implementation("io.netty:netty-all:4.1.115.Final")

    implementation(kotlin("reflect"))

    implementation("ai.koog:agents-core:1.1.1")
    implementation("ai.koog:agents-ext:1.1.1-beta")
    implementation("ai.koog:agents-features-event-handler:1.1.1")

    implementation("ai.koog:prompt-executor-google-client:1.1.1-beta")
    implementation("ai.koog:prompt-executor-ollama-client:1.1.1")
    runtimeOnly("ai.koog:prompt-executor-openai-client:1.1.1")
    implementation("ai.koog:koog-ktor-jvm:1.1.1-beta")

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")

    val grpcVersion = "1.80.0"
    implementation(platform("io.grpc:grpc-bom:$grpcVersion"))
    implementation("io.grpc:grpc-netty")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-services")

    implementation("io.grpc:grpc-kotlin-stub:1.4.3")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.5")

    implementation(platform("io.insert-koin:koin-bom:4.1.1"))
    implementation("io.insert-koin:koin-core")
    implementation("io.insert-koin:koin-ktor")


    implementation(platform("org.mongodb:mongodb-driver-bom:5.7.0"))
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine")
    implementation("org.mongodb:bson-kotlin")

    implementation("ch.qos.logback:logback-classic:1.5.12")
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "io.grpc" && requested.name != "grpc-kotlin-stub") {
                useVersion("1.80.0")
            }
        }
    }
}

tasks.shadowJar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}