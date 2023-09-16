import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.5.1"
    id("org.jetbrains.dokka") version "1.9.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    id("com.squareup.sqldelight") version "1.5.5"
}

group = "averysimonsen.los"
version = "1.0-SNAPSHOT"

allprojects {
    repositories() {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.3")
    implementation(compose.desktop.currentOs)
    implementation("com.squareup.sqldelight:sqlite-driver:1.5.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation(project(mapOf("path" to ":ipv8")))
    implementation("org.ini4j:ini4j:0.5.4")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "shoot2"
            packageVersion = "1.0.0"
        }
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
