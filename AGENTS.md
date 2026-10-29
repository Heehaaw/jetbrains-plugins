# Repository rules

- Keep each JetBrains plugin in its own top-level Gradle subproject folder; share the root wrapper, settings, and plugin versions. Keep shared release configuration in the root Gradle build and workflows in `.github/workflows/`.
- Read the plugin's README before editing. Keep Kotlin packages shallow and changes focused.
- Preserve plugin IDs and existing editor and terminal behavior. The Terminal APIs are experimental; verify compatibility against the supported IDE builds.
- Do not add Codex invocation-level configuration overrides without checking shared-server compatibility. The former notification override prevented shared-server resumes.
- Run plugin tests and the relevant plugin verifier after implementation changes. Run the root `verifyRelease` task against a retained plugin ZIP after release-configuration changes.
- Follow [release guidelines](doc/release-guidelines.md). Main CI retains installable ZIPs; manual preparation promotes tested artifacts into a draft. Publication stays manual.
- Keep Git identity and signing configuration local to this repository. Never expose credentials or private keys.
