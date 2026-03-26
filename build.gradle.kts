import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    maven("https://repo.codemc.io/repository/maven-releases/") // CommandAPI Repository
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")

    // CommandAPI in das Shadow-JAR packen, damit zur Laufzeit alle Klassen verfügbar sind
    implementation("dev.jorel", "commandapi-paper-shade", commandAPIVersion)
    implementation("dev.jorel", "commandapi-kotlin-paper", commandAPIVersion)
}


tasks {
    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    compileKotlin {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        archiveBaseName.set("Challenge-Kittys")
        archiveVersion.set(version.toString())
        archiveClassifier.set("")

        mergeServiceFiles()
        // Minimieren kann manchmal zu Problemen führen (entfernt benötigte Klassen); setze es aus, falls Probleme auftreten.
        // minimize()
    }

    runServer {
        minecraftVersion("1.21.11")
    }
}