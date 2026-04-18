import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("com.gradleup.shadow") version "9.2.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val commandAPIVersion = "11.1.0"

group = "io.github.black_Kittys22"
version = "3.1"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://m2.dv8tion.net/releases")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    // PaperMC
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")

    // CommandAPI
    implementation("dev.jorel", "commandapi-paper-shade", commandAPIVersion)
    implementation("dev.jorel", "commandapi-kotlin-paper", commandAPIVersion)

    // JDA (Discord-Bot)
    implementation("net.dv8tion:JDA:5.0.0-beta.20") {
        exclude(group = "org.json", module = "json")
        exclude(group = "club.minnced", module = "opus-java")
    }

    // Adventure API
    implementation("net.kyori:adventure-text-minimessage:4.14.0")
    implementation("net.kyori:adventure-api:4.14.0")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",
            "-Xallow-kotlin-package",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(21)
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.withType<ShadowJar>().configureEach {
    archiveBaseName.set("Challenge-Kittys")
    archiveVersion.set(version.toString())
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    archiveBaseName.set("Challenge-Kittys")
    archiveVersion.set(version.toString())
    archiveClassifier.set("")
    mergeServiceFiles()
}