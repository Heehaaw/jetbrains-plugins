# Release guidelines

The repository hosts all plugins; each plugin has its own folder and build. Keep installable bundles separate. Release preparation follows the sibling `jellyfin-plugins`
repository's tested-artifact promotion flow.

1. Run the plugin tests, compatibility verifier, and release-tooling tests. Commit with this repository's GitHub signing identity and push main when authorized.
2. Wait for successful main push CI for that exact commit. The workflow retains the installable development ZIP for 90 days.
3. Run **Actions → Prepare release** from the tested branch. Choose patch, minor, major, or a custom `vMAJOR.MINOR.PATCH` tag. Automatic bumps use the highest published stable release.
4. Follow the workflow summary's draft link. Review the versioned ZIP and `SHA256SUMS`, then publish the draft manually.

Release preparation rejects a commit without successful main push CI, expired or missing artifacts, workflow files that differ from current main, an existing release tag, or a tag
pointing to another commit. Preparation jobs run serially. A tag push alone never publishes a release.

The plugin's source version stays `0.0.0-dev`. Promotion updates `META-INF/plugin.xml` and the `Version` field in the retained plugin JAR's manifest, then renames the JAR and ZIP
to the selected release version. Every other JAR entry, including compiled classes, and every bundled dependency retains its bytes. The checksum describes the final ZIP.
Preparation does not rebuild plugin code, rerun the plugin tests, or require a JetBrains Marketplace token. It creates a draft rather than publishing to Marketplace.

For the first imported Codex Terminal Context release, use **custom → v0.5.0** to continue its existing local version sequence. Later automatic bumps use published releases.

To exercise promotion locally with an existing development ZIP:

```sh
python3 scripts/promote.py codex-terminal-context/build/distributions/codex-terminal-context-0.0.0-dev.zip v0.5.0 --output-dir artifacts/release
```

Local installation and published release verification are separate. Check the loaded plugin version, toolbar actions, editor references, and Codex resume behavior in the intended
IDE before claiming runtime behavior is verified. Record any verification still pending.
