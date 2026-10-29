# Codex Terminal Context

JetBrains IDE plugin that inserts the current editor file or selected line range into a Codex CLI session in the IDE's native Terminal.

The implementation is Kotlin-only and keeps its feature-oriented source in the flat `codexcontext` package.

## Usage

1. Open a file and optionally select the relevant lines.
2. Choose **Send Context to Codex Terminal** from the main toolbar, Tools menu, editor context menu, or floating selection toolbar.

The plugin opens a native Codex terminal when needed, inserts a reference such as `src/Example.cs:12-18 ` without submitting it, then focuses that terminal. When several Codex
sessions are open, select the target terminal tab first.

Use **Open Codex Terminal** in the Terminal toolbar or Tools menu to start a session without an editor reference.

Codex launches without configuration overrides so it can use its shared background server and resume its conversations. Completion popups were removed because their `-c notify=...`
override forced embedded mode in Codex 0.165 and prevented resuming conversations owned by the shared server.

The action has no default shortcut so it does not conflict with the installed Claude Code or Codex Launcher plugins. Assign one under **Settings → Keymap → Plugins → Codex Terminal
Context**.

## Compatibility

The same plugin artifact supports JetBrains IDEs on the 2026.2 platform or newer when the IDE bundles the `org.jetbrains.plugins.terminal` plugin. This includes Rider and IntelliJ
IDEA; other desktop IDEs use the same platform dependency contract. Remote clients and IDE products without the native Terminal plugin are outside its scope.

Build `262` is the minimum because the implementation uses the reworked Terminal API that backs the native AI-agent launcher. JetBrains currently marks that API as experimental, so
each new platform line should be checked with the plugin verifier.

## Development

The plugin compiles against IntelliJ IDEA 2026.2 by default and requires JDK 25.

Build against a locally installed JetBrains IDE to avoid downloading another IDE distribution:

```bash
./gradlew -PlocalIdePath="/Users/example/Applications/IntelliJ IDEA.app" test buildPlugin verifyPlugin
```

Without `localIdePath`, the build resolves the IntelliJ Platform version pinned in `gradle.properties`. To verify one artifact against several local IDEs, pass their paths
separated by the operating system path separator:

```bash
./gradlew -PverificationIdePaths="/Applications/IntelliJ IDEA.app:/Applications/Rider.app" verifyPlugin
```

The installable ZIP is written under `build/distributions/`.

Development builds use `0.0.0-dev`. The repository's [release workflow](../doc/release-guidelines.md) promotes the tested ZIP to the selected release tag without recompiling it.
