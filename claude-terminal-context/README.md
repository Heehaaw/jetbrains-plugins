# Claude Terminal Context

JetBrains IDE plugin that inserts the current editor file or selected line range into a Claude CLI session in the IDE's native Terminal.

The implementation is Kotlin-only and keeps its feature-oriented source in the flat `claudecontext` package.

## Usage

1. Open a file and optionally select the relevant lines.
2. Choose **Send Context to Claude Terminal** from the main toolbar, Tools menu, editor context menu, or floating selection toolbar.

The plugin opens a native Claude terminal when needed, inserts a reference such as `@src/Example.cs#L12-18 ` without submitting it, then focuses that terminal. When several Claude
sessions are open, select the target terminal tab first.

Use **Open Claude Terminal** in the Terminal toolbar or Tools menu to start a session without an editor reference.

Install the Claude Code CLI first. The plugin finds `claude` on PATH or in `~/.local/bin` and launches it without extra arguments.

The action has no default shortcut so it does not conflict with the installed Claude Code or Codex Launcher plugins. Assign one under **Settings → Keymap → Plugins → Claude Terminal
Context**.

References use [Claude Code file-reference syntax](https://code.claude.com/docs/en/jetbrains). Like the Codex plugin, this plugin reuses dedicated agent tabs; it does not detect Claude started inside an ordinary shell tab.

## Compatibility

The same plugin artifact supports JetBrains IDEs on the 2026.2 platform or newer when the IDE bundles the `org.jetbrains.plugins.terminal` plugin. This includes Rider and IntelliJ
IDEA; other desktop IDEs use the same platform dependency contract. Remote clients and IDE products without the native Terminal plugin are outside its scope.

Build `262` is the minimum because the implementation uses the reworked Terminal API that backs the native AI-agent launcher. JetBrains currently marks that API as experimental, so
each new platform line should be checked with the plugin verifier.

## Development

The plugin compiles against IntelliJ IDEA 2026.2 by default and requires JDK 25. Run the following commands from the repository root using the shared Gradle wrapper.

Build against a locally installed JetBrains IDE to avoid downloading another IDE distribution:

```bash
./gradlew -PlocalIdePath="/Users/example/Applications/IntelliJ IDEA.app" :claude-terminal-context:test :claude-terminal-context:buildPlugin :claude-terminal-context:verifyPlugin
```

Without `localIdePath`, the build resolves the IntelliJ Platform version pinned in `gradle.properties`. To verify one artifact against several local IDEs, pass their paths
separated by the operating system path separator:

```bash
./gradlew -PverificationIdePaths="/Applications/IntelliJ IDEA.app:/Applications/Rider.app" :claude-terminal-context:verifyPlugin
```

The installable ZIP is written under `claude-terminal-context/build/distributions/`.

Development builds use `0.0.0-dev`. The repository's [release workflow](../doc/release-guidelines.md) promotes the tested ZIP to the selected release tag without recompiling it.
