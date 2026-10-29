import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import dev.bmac.gradle.intellij.UpdateXmlTask
import org.jetbrains.intellij.platform.gradle.tasks.PatchPluginXmlTask
import java.util.zip.ZipFile

plugins {
    base
    kotlin("jvm") version "2.3.20" apply false
    id("org.jetbrains.intellij.platform") version "2.19.0" apply false
    id("dev.bmac.intellij.plugin-uploader") version "1.3.5"
}

repositories { mavenCentral() }

tasks.assemble { dependsOn(subprojects.map { "${it.path}:buildPlugin" }) }
tasks.check { dependsOn(subprojects.flatMap { listOf("${it.path}:check", "${it.path}:verifyPlugin") }) }

val releaseVersion = providers.gradleProperty("releaseTag").map { tag ->
    require(Regex("v(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matches(tag)) { "Use -PreleaseTag=vMAJOR.MINOR.PATCH." }
    tag.removePrefix("v")
}
val repository = providers.gradleProperty("pluginRepository")
    .orElse(providers.environmentVariable("GITHUB_REPOSITORY")).orElse("Heehaaw/jetbrains-plugins")
val inputDirectory = layout.projectDirectory.dir(providers.gradleProperty("inputDirectory").getOrElse("artifacts/input"))
val outputDirectory = layout.projectDirectory.dir(providers.gradleProperty("outputDirectory").getOrElse("artifacts/release"))

val retainedPlugins = fileTree(inputDirectory).matching { include("*.zip") }.files.sorted().map { archive ->
    val result = IdePluginManager.createManager().createPlugin(archive.toPath())
    require(result is PluginCreationSuccess) { "Invalid retained plugin ${archive.name}: $result" }
    // The descriptor is held in memory; release parser-owned archive handles after inspection.
    result.resources.forEach { it.close() }
    archive to result.plugin
}
require(retainedPlugins.map { it.second.pluginId }.distinct().size == retainedPlugins.size) { "Retained bundles must have distinct plugin IDs." }
val resetCatalog = tasks.register<Delete>("resetPluginRepository") { delete(outputDirectory.file("updatePlugins.xml")) }
var previousCatalogTask: TaskProvider<UpdateXmlTask>? = null
val catalogTasks = retainedPlugins.mapIndexed { index, (archive, plugin) ->
    val bundledJar = zipTree(archive).matching { include("*/lib/*.jar") }.files.single { jar ->
        ZipFile(jar).use { it.getEntry("META-INF/plugin.xml") != null }
    }
    val rootDirectory = bundledJar.parentFile.parentFile.name
    val jarDirectory = "$rootDirectory/lib"
    val untilBuildNumber = plugin.underlyingDocument.rootElement.getChild("idea-version")?.getAttributeValue("until-build")
    val patchXml = tasks.register<PatchPluginXmlTask>("patchReleaseDescriptor$index") {
        inputFile.set(resources.text.fromArchiveEntry(bundledJar, "META-INF/plugin.xml").asFile())
        outputFile.set(layout.buildDirectory.file("release/plugin$index/plugin.xml"))
        pluginVersion.set(releaseVersion)
        untilBuildNumber?.let(untilBuild::set)
    }
    val packageJar = tasks.register<Jar>("packageReleaseJar$index") {
        from(zipTree(bundledJar)) { exclude("META-INF/plugin.xml", "META-INF/MANIFEST.MF") }
        from(patchXml.flatMap { it.outputFile }) { into("META-INF") }
        manifest.from(resources.text.fromArchiveEntry(bundledJar, "META-INF/MANIFEST.MF").asFile()) {
            eachEntry { if (key == "Version") value = releaseVersion.get() }
        }
        archiveBaseName.set(rootDirectory)
        archiveVersion.set(releaseVersion)
        destinationDirectory.set(layout.buildDirectory.dir("release/plugin$index"))
    }
    val packageZip = tasks.register<Zip>("packageReleaseZip$index") {
        from(zipTree(archive)) { exclude("$jarDirectory/${bundledJar.name}") }
        from(packageJar.flatMap { it.archiveFile }) { into(jarDirectory) }
        archiveBaseName.set(rootDirectory)
        archiveVersion.set(releaseVersion)
        destinationDirectory.set(outputDirectory)
    }
    val earlierCatalogTask = previousCatalogTask
    tasks.register<UpdateXmlTask>("updatePluginRepository$index") {
        dependsOn(packageZip, resetCatalog)
        earlierCatalogTask?.let { mustRunAfter(it) }
        updateFile.set(outputDirectory.file("updatePlugins.xml"))
        downloadUrl.set(releaseVersion.map { "https://github.com/${repository.get()}/releases/download/v$it/$rootDirectory-$it.zip" })
        pluginId.set(plugin.pluginId)
        pluginName.set(plugin.pluginName)
        version.set(releaseVersion)
        pluginDescription.set(plugin.description)
        changeNotes.set(plugin.changeNotes)
        sinceBuild.set(requireNotNull(plugin.sinceBuild) { "Retained plugin must declare since-build." }.asString())
        untilBuildNumber?.let(untilBuild::set)
    }.also { previousCatalogTask = it }
}

tasks.register("prepareRelease") {
    group = "publishing"
    description = "Promote retained plugin ZIPs and generate the JetBrains repository without compiling."
    dependsOn(catalogTasks)
    doFirst { require(retainedPlugins.isNotEmpty()) { "Place retained plugin ZIPs in artifacts/input or set -PinputDirectory." } }
}
tasks.register("verifyRelease") {
    group = "verification"
    description = "Exercise artifact-only release preparation."
    dependsOn("prepareRelease")
}
