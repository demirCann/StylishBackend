val exposed_version: String by project
val h2_version: String by project
val koin_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val ktor_version: String = "3.1.1"
val mockk_version = "1.13.9"
val testcontainers_version = "1.19.6"

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.1"
    kotlin("plugin.serialization") version "2.1.10"
}

group = "demircandemir.com"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation("io.ktor:ktor-server-openapi:$ktor_version")
    implementation("io.ktor:ktor-server-swagger:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    
    // Flyway dependencies
    implementation("org.flywaydb:flyway-core:11.2.0")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.2.0")

    // Test dependencies
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("io.mockk:mockk:$mockk_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.insert-koin:koin-test:$koin_version")
    testImplementation("io.insert-koin:koin-test-junit5:$koin_version")
    testImplementation("org.testcontainers:postgresql:$testcontainers_version")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    testImplementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    testImplementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
}

tasks.test {
    useJUnitPlatform()
}
