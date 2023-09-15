pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("jvm").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}

rootProject.name = "shoot2"

include(":ipv8", ":tracker", ":ipv8-jvm")
project(":ipv8").projectDir = file("kotlin-ipv8/ipv8")
project(":tracker").projectDir = file("kotlin-ipv8/tracker")
project(":ipv8-jvm").projectDir = file("kotlin-ipv8/ipv8-jvm")
