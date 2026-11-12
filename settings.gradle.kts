pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "jetbrains-plugins"

include("codex-terminal-context", "claude-terminal-context", "t3-code-context")
