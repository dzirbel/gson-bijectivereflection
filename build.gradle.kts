import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private object Versions {
    const val detekt = "1.15.0-RC1" // https://github.com/detekt/detekt; also update plugin version
    const val findBugs = "3.0.2" // https://mvnrepository.com/artifact/com.google.code.findbugs/jsr305
    const val gson = "2.8.6" // https://github.com/google/gson
    const val junit = "5.7.0" // https://junit.org/junit5/
    const val kotlinReflect = "1.4.21" // https://kotlinlang.org/docs/reference/reflection.html
    const val truth = "1.1" // https://truth.dev/
}

plugins {
    `java-library`

    // https://kotlinlang.org/releases.html
    kotlin("jvm") version "1.4.21"

    // https://github.com/detekt/detekt; also update dependency version
    id("io.gitlab.arturbosch.detekt") version "1.15.0-RC1"
}

version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("com.google.code.gson", "gson", Versions.gson)
    implementation("org.jetbrains.kotlin", "kotlin-reflect", Versions.kotlinReflect)
    implementation("com.google.code.findbugs", "jsr305", Versions.findBugs)

    testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", Versions.junit)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.junit)
    testImplementation("com.google.truth", "truth", Versions.truth)

    detektPlugins("io.gitlab.arturbosch.detekt", "detekt-formatting", Versions.detekt)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.jvmTarget = "1.8"
}

configurations.all {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
    }
}

detekt {
    input = files("src/main/kotlin", "src/test/kotlin")
    config = files("detekt-config.yml")
}
