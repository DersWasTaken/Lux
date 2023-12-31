import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    idea
    java
    kotlin("jvm") version "1.9.10"
    id("maven-publish")
}

group = "me.ders"
version = "0.0.1"


repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("dev.hollowcube:minestom-ce:8a9029c80c")
    implementation("net.kyori:adventure-text-serializer-ansi:4.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("net.kyori:adventure-text-minimessage:4.14.0")
}

tasks.named("compileKotlin", KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.DersWasTaken"
            artifactId = "Lux"
            version = "0.0.1"
        }
    }
}