import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.janmilota"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val localIdePath = providers.gradleProperty("localIdePath").orNull
        if (localIdePath != null) {
            local(localIdePath)
        } else {
            intellijIdea(providers.gradleProperty("platformVersion"))
        }

        bundledPlugin("org.jetbrains.plugins.terminal")
    }

    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
        }
    }

    pluginVerification {
        ides {
            val localVerificationIdePaths = providers.gradleProperty("verificationIdePaths")
                .orNull
                ?.split(File.pathSeparator)
                ?.filter(String::isNotBlank)

            if (localVerificationIdePaths.isNullOrEmpty()) {
                current()
            } else {
                localVerificationIdePaths.forEach { path -> local(file(path)) }
            }
        }
    }
}
