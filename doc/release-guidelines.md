# Release guidelines

The repository hosts all plugins as subprojects of one root Gradle build. Each plugin has its own folder and module configuration. Keep installable bundles separate. Release preparation follows the sibling `jellyfin-plugins`
repository's tested-artifact promotion flow.

1. Run the plugin tests, compatibility verifier, and Gradle release verification. Commit with this repository's GitHub signing identity and push main when authorized.
2. Wait for successful main push CI for that exact commit. The workflow retains the installable development ZIP for 90 days.
3. Run **Actions → Prepare release** from the tested branch. Choose patch, minor, major, or a custom `vMAJOR.MINOR.PATCH` tag. Automatic bumps use the highest published stable release.
4. Follow the workflow summary's draft link. Review the versioned ZIP, `updatePlugins.xml`, and `SHA256SUMS`, then publish the draft manually as Latest.

Release preparation rejects a commit without successful main push CI, expired or missing artifacts, workflow files that differ from current main, an existing release tag, or a tag
pointing to another commit. Preparation jobs run serially. A tag push alone never publishes a release.

The plugin's source version stays `0.0.0-dev`. The root Gradle release build uses JetBrains' `PatchPluginXmlTask` to update `META-INF/plugin.xml`, then standard `Jar` and `Zip` tasks to update the manifest version and package
the retained binaries under the selected release version. Every other JAR entry, including compiled classes, and every bundled dependency retains its bytes. The checksum describes the final ZIP.
The published `dev.bmac.intellij.plugin-uploader` plugin's `UpdateXmlTask` generates `updatePlugins.xml` from the retained bundle metadata and release version. Each catalog entry uses the packaged plugin's identity, version, and IDE compatibility bounds, with an HTTPS
URL for that release's ZIP. `SHA256SUMS` covers both the ZIPs and catalog. Preparation does not rebuild plugin code, rerun the plugin tests, or require a JetBrains Marketplace token.

The custom plugin repository URL is:

```text
https://github.com/Heehaaw/jetbrains-plugins/releases/latest/download/updatePlugins.xml
```

It becomes available after the first draft is published as Latest. Draft preparation leaves the current public plugin source unchanged. See the
[installation instructions](../README.md#install-from-the-plugin-repository) for adding the source to Rider or IntelliJ IDEA.

For the first imported Codex Terminal Context release, use **custom → v0.5.0** to continue its existing local version sequence. Later automatic bumps use published releases.

To exercise promotion and verification locally with an existing development ZIP:

```sh
./gradlew --no-daemon --build-cache verifyRelease -PreleaseTag=v0.5.0 -PinputDirectory=codex-terminal-context/build/distributions
```

The root Gradle build reads every plugin ZIP in `inputDirectory`, which defaults to `artifacts/input/`. `prepareRelease` patches and repackages retained artifacts and generates
the catalog. `verifyRelease` is the CI entry point for that promotion flow. Neither task compiles plugin sources.

Outputs default to `artifacts/release/`; use `-PoutputDirectory=/path/to/output` to change them. `-PpluginRepository=owner/repository` overrides `GITHUB_REPOSITORY` for the
catalog's GitHub download URLs. Local runs default to `Heehaaw/jetbrains-plugins`. Release preparation restores the Gradle cache in read-only mode; main CI owns cache writes.

Local installation and published release verification are separate. Check the loaded plugin version, toolbar actions, editor references, and Codex resume behavior in the intended
IDE before claiming runtime behavior is verified. Record any verification still pending.
