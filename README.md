# JetBrains plugins

Independent JetBrains IDE plugins, maintained in one repository. Each plugin owns its implementation, tests, and subproject build configuration in its own folder. They share one root Gradle wrapper, settings, and plugin versions.

| Plugin | Purpose |
| --- | --- |
| [Codex Terminal Context](codex-terminal-context/) | Open Codex in the native Terminal and insert editor file or selected-line references without submitting them. |

## Install from the plugin repository

1. Open **Settings → Plugins**, then choose **Manage Plugin Repositories** from the settings menu.
2. Add this repository URL:

   ```text
   https://github.com/Heehaaw/jetbrains-plugins/releases/latest/download/updatePlugins.xml
   ```

3. Search for **Codex Terminal Context** in Plugins, install it, and restart the IDE if prompted.

The repository URL becomes available after the first release is published as Latest. Each later Latest release updates the catalog.

## Build

Use JDK 25 and run from the repository root:

```sh
./gradlew --build-cache build
```

The plugin README describes building and verifying against locally installed Rider or IntelliJ IDEA versions. Install its ZIP from `codex-terminal-context/build/distributions/` through
**Settings → Plugins → Install Plugin from Disk**.

## CI and releases

Main and pull-request CI run the plugin tests, compatibility verifier, and Gradle release verification, then retain the installable development ZIP for 90 days.

After main CI passes, open **Actions → Prepare release → Run workflow**. Select the tested branch and choose a patch, minor, major, or custom `vMAJOR.MINOR.PATCH` version.
The workflow downloads the successful main build's exact ZIP, updates its plugin version metadata, and creates a draft release with the ZIP, JetBrains repository catalog, and
checksums. It preserves compiled classes and bundled dependencies. Missing or expired artifacts stop preparation. Review the linked draft in Releases and publish it manually as
Latest. Tag pushes do not publish releases.

Release tags are the version source of truth. Development builds use `0.0.0-dev`; no version-bump commit is needed after publication. See [release guidelines](doc/release-guidelines.md).

The root Gradle release build uses JetBrains' `PatchPluginXmlTask`, standard Gradle archive tasks, and the published `dev.bmac.intellij.plugin-uploader` plugin's `UpdateXmlTask`.
It promotes retained artifacts without compiling plugin sources and generates the repository catalog. To run it locally after building a development ZIP:

```sh
./gradlew --no-daemon --build-cache verifyRelease -PreleaseTag=v0.5.0 -PinputDirectory=codex-terminal-context/build/distributions
```

Release ZIPs and `updatePlugins.xml` are written to `artifacts/release/`. Each plugin is a subproject of the shared root build. Release preparation compiles no plugin sources.

## License

MIT. JetBrains, Codex, and bundled dependencies retain their own licenses.
