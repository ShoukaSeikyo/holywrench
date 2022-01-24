import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://raw.githubusercontent.com/TerraformersMC/Archive/main/releases/")
        maven(url = "https://maven.shedaniel.me/")
    }
}

plugins {
    kotlin("jvm") version "1.4.31"
    id("fabric-loom") version "0.10-SNAPSHOT"
    `kotlin-dsl`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    minecraft("com.mojang:minecraft:1.18.1")
    mappings("net.fabricmc:yarn:1.18.1+build.4:v2")
//    mappings (minecraft.officialMojangMappings())

    implementation(gradleApi())
    implementation("com.google.code.gson:gson:2.8.9")

    modImplementation("net.fabricmc:fabric-loader:0.12.11")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.44.0+1.18")

    modImplementation("net.fabricmc:fabric-language-kotlin:1.6.1+kotlin.1.5.10")

    compileOnly ("org.projectlombok:lombok:1.18.22")
    annotationProcessor ("org.projectlombok:lombok:1.18.22")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.4"
    jvmTarget = "17"
    freeCompilerArgs = listOf("-Xjvm-default=all")
}