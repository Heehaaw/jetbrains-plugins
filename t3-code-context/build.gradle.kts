import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.VerificationReportsFormats
import java.io.File

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
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
        // Keep detailed diagnostics in saved reports; the verifier still enforces compatibility failures.
        verificationReportsFormats.set(listOf(VerificationReportsFormats.HTML))

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
