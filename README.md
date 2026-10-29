# JetBrains plugins

Independent JetBrains IDE plugins, maintained in one repository. Each plugin owns its implementation, tests, Gradle wrapper, and build configuration in its own folder.

| Plugin | Purpose |
| --- | --- |
| [Codex Terminal Context](codex-terminal-context/) | Open Codex in the native Terminal and insert editor file or selected-line references without submitting them. |

## Build

Codex Terminal Context requires JDK 25. From its folder:

```sh
cd codex-terminal-context
./gradlew test buildPlugin verifyPlugin
```

The plugin README describes building and verifying against locally installed Rider or IntelliJ IDEA versions. Install its ZIP from `build/distributions/` through
**Settings → Plugins → Install Plugin from Disk**.

## CI and releases

Main and pull-request CI run the plugin tests, compatibility verifier, and release-tooling tests, then retain the installable development ZIP for 90 days.

After main CI passes, open **Actions → Prepare release → Run workflow**. Select the tested branch and choose a patch, minor, major, or custom `vMAJOR.MINOR.PATCH` version.
The workflow downloads the successful main build's exact ZIP, updates its plugin version metadata, and creates a draft release with the ZIP and checksums. It preserves compiled
classes and bundled dependencies. Missing or expired artifacts stop preparation. Review the linked draft in Releases and publish it manually. Tag pushes do not publish releases.

Release tags are the version source of truth. Development builds use `0.0.0-dev`; no version-bump commit is needed after publication. See [release guidelines](doc/release-guidelines.md).

Run the release-tooling tests separately with:

```sh
python3 -m unittest discover -s tests -v
```

## License

MIT. JetBrains, Codex, and bundled dependencies retain their own licenses.
