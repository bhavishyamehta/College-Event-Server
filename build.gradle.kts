plugins {
    kotlin("jvm") version "2.0.21"
    id("io.ktor.plugin") version "3.0.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    // Ktor Core
    implementation("io.ktor:ktor-server-core-jvm:2.3.0")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.0")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.0")
    implementation("io.ktor:ktor-server-auth:2.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Ktor Features
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.0")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.0")
    implementation("io.ktor:ktor-server-config-yaml-jvm:2.3.0")
    implementation("io.ktor:ktor-client-cio:2.3.0")

    // Status pages / CORS
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.41.1")
    implementation("org.postgresql:postgresql:42.5.1")

    // JWT
    implementation("com.auth0:java-jwt:4.4.0")

    // Google Maps
    implementation("com.google.maps:google-maps-services:2.1.2")

    // Ktor client
    implementation("io.ktor:ktor-client-core:2.3.0")
    implementation("io.ktor:ktor-client-cio:2.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")


    // ── MySQL driver + connection pool ────────────────────────────────────────
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Database drivers
    implementation("org.postgresql:postgresql:42.7.2")
    //implementation("com.h2database:h2:$h2Version")  // for local dev/testing

    // Password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // Firebase Admin (for push notifications)
    implementation("com.google.firebase:firebase-admin:9.2.0")

    implementation("io.ktor:ktor-server-websockets:1.6.0")

    // Fix Firebase dependency issue
    implementation("com.google.guava:guava:32.1.2-jre")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.21")

    // Add these Jackson dependencies
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
}
