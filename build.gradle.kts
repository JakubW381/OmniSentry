plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.jetbrains.kotlin.plugin.jpa") version "2.3.21" apply false
    id("com.google.protobuf") version "0.10.0" apply false
}

group = "dev.jakubw.omnisentry"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}