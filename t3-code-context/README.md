# T3 Code Context

JetBrains IDE plugin that pastes the current editor file or selected line range into the thread open in the [T3 Code](https://github.com/pingdotgg/t3code) desktop app on macOS.

The implementation is Kotlin-only and keeps its feature-oriented source in the flat `t3context` package.

## Usage

1. Start T3 Code and open the target thread with the cursor in its message box.
2. In the IDE, open a file and optionally select the relevant lines.
3. Choose **Send Context to T3 Code** from the main toolbar, Tools menu, editor context menu, or floating selection toolbar.

The plugin copies a reference such as `@src/Example.cs#L12-18 `, brings T3 Code to the front, and pastes it with ⌘V without sending it. It restores the previous clipboard
afterwards. When pasting fails, the reference stays on the clipboard and a notification explains the cause.

The action has no default shortcut. Assign one under **Settings → Keymap → Plugins → T3 Code Context**.

## How it works and limits

T3 Code has no API for adding text to the open thread's draft: prompt links replace the draft, and `t3 thread send` submits a message. The plugin therefore runs an AppleScript
through `osascript` that activates T3 Code (bundle ID `com.t3tools.t3code`) and sends ⌘V through System Events.

- The first run asks to let the IDE control System Events. The IDE also needs an entry under **System Settings → Privacy & Security → Accessibility**.
- The reference lands wherever focus was in the T3 Code window. T3 Code has no command that focuses the message box first.
- T3 Code must already be running; the plugin does not launch it.
- Clipboard history tools record the reference.
- References are relative to the project directory, or absolute for files outside it. T3 Code passes them to the agent as plain text.
- The action is hidden on Windows and Linux.

## Compatibility

The same plugin artifact supports JetBrains IDEs on the 2026.2 platform or newer, including Rider and IntelliJ IDEA. It depends only on the platform module.

## Development

The plugin compiles against IntelliJ IDEA 2026.2 by default and requires JDK 25. Run the following commands from the repository root using the shared Gradle wrapper.

Build against a locally installed JetBrains IDE to avoid downloading another IDE distribution:

```bash
./gradlew -PlocalIdePath="/Users/example/Applications/IntelliJ IDEA.app" :t3-code-context:test :t3-code-context:buildPlugin :t3-code-context:verifyPlugin
```

Without `localIdePath`, the build resolves the IntelliJ Platform version pinned in `gradle.properties`. To verify one artifact against several local IDEs, pass their paths
separated by the operating system path separator:

```bash
./gradlew -PverificationIdePaths="/Applications/IntelliJ IDEA.app:/Applications/Rider.app" :t3-code-context:verifyPlugin
```

The installable ZIP is written under `t3-code-context/build/distributions/`.

Development builds use `0.0.0-dev`. The repository's [release workflow](../doc/release-guidelines.md) promotes the tested ZIP to the selected release tag without recompiling it.
