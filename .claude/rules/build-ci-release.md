---
paths:
  - "*.gradle.kts"
  - "*/build.gradle.kts"
  - "*/gradle.lockfile"
  - "gradle.properties"
  - "gradle/**"
  - "config/detekt/**"
  - ".github/**"
  - "lefthook.yml"
  - "scripts/hooks/**"
  - "scripts/ci/**"
  - "api-dump/**"
  - "LICENSE"
---

# Build, CI, scanning and releases — component brief

The Gradle build (`settings.gradle.kts`, the root and module `build.gradle.kts`, `gradle/libs.versions.toml`,
`gradle/verification-metadata.xml`, `gradle/osv-scanner.toml`, the lockfiles), the five workflows in
`.github/workflows/`, the lefthook hooks in `scripts/hooks/`, `scripts/ci/`, `:api-dump`, and what a release will
need: publishing, signing, the licence. The toolchain is encri's, so most of this is encri's `build-ci-release` brief
with what differs here. This brief is level 1; full records are in `docs/decisions/`.

## Rules that hold here
- **The toolchain is encri's, version for version**: the Gradle 9.7.1 wrapper, AGP 9.4.1, ktlint 1.8.0, detekt 1.23.8,
  Kover 0.9.9, JUnit 5.14.4, the daemon pin, `.editorconfig`, the detekt deltas, the hooks and the pinned action SHAs.
  encri compiles its service against the contract, so a bump lands in both repositories or in neither. (L3)
- **Kotlin 2.4.21 is on the root build's classpath**, so AGP's built-in Kotlin compiles with it, for
  GHSA-r937-wjx7-w2jp in the Kotlin build cache. The same move is encri's D119. (L9)
- **Published modules compile their main sources at language and API version 2.2 and declare kotlin-stdlib 2.2.10**
  (`coreLibrariesVersion`), so an app needs Kotlin 2.1. Tests use what they like. (L17)
- **Group `com.systemhalted.encri`, version `0.1.0-SNAPSHOT`, each project named after its artifact**, because a
  composite build substitutes on `group:project name`: encri and Encri Files include this build rather than resolve
  it (encri D115, D126). Renaming the repository changed only the root project's name. (L1, L6, L19)
- **minSdk 27, a hard limit, and explicit API mode** in every published module. (L1)
- **`allUnitTests` is the aggregate** that CI and the pre-push hook run, so a module whose tests have another name is
  never skipped (encri D11). Robolectric's SDK 36 runs need `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`
  on JDK 21, on the test task. (L8)
- **Coverage gates**: 90% line on `encri-contract`; 90% line and 85% branch on `encri-client` and `encri-registry`;
  diff coverage at least 85% on changed lines, by `scripts/ci/diff-coverage.py`. The aidl tool's classes are excluded
  by name. No module is exempt. (L4, L11)
- **`ci.yml`**: ktlint, detekt, `allUnitTests`, the registry's validator, the append-only check against the newest
  `v*` tag, `assemble` (the release AAR is what a client links), `koverVerify` and diff coverage. (L3, L15, L18)
- **`codeql.yml`** analyses `java-kotlin`, built by the workflow with `assembleRelease`, `--no-build-cache` and the
  Kotlin compiler in-process, and `actions` with no build, at `security-extended`, on pull requests, pushes to
  `master` and weekly. Its checkout does not persist the token. (L7, L9)
- **`dependencies.yml`: what ships is a gate, what builds it is a report.** OSV-Scanner reads each module's
  `gradle.lockfile` with no triage file and fails on any advisory. It also reads every artifact the verification
  metadata pins against `gradle/osv-scanner.toml`, reporting to the Security tab; each entry there says why its flaw
  cannot be reached and expires on 2027-01-22. The scanner is a release binary checked against Google's SHA-256, not
  the action's Docker image, and a scan that did not run fails the job. (L10)
- **Each module's `releaseRuntimeClasspath` is locked, STRICT**, in `gradle.lockfile` beside it, and changes only by
  `./gradlew assembleRelease --write-locks`. (L10)
- **Regenerate the verification metadata from an empty file, with `--rerun-tasks`**: a merge keeps what the build
  stopped using, and a task that is up to date resolves nothing. It is `sha256` with signatures off, so it freezes the
  set rather than attests it (encri D7). (L9, `docs/conventions.md`)
- **Prefer what encri's verified set already holds.** UiAutomator and the binary-compatibility validator were each
  rejected for bringing artifacts it does not; Robolectric, the androidx test runner and kotlin-reflect came in at
  encri's versions. (L8, L10, L13, L16)
- **The API dumps come from `:api-dump`**, a module of its own that reads the published modules through kotlin-reflect
  2.4.21, test-only and never published. `-PupdateApi` writes `api/*.api`. (L16)
- **Hooks are lefthook's** and `core.hooksPath` stays unset: signing preflight, ktlint and detekt on commit;
  Conventional Commits of at most 72 characters; signatures and `allUnitTests` on push. Every rule is enforced again in
  CI, because `--no-verify` exists. (`docs/conventions.md`)
- **`commit-policy.yml` checks a push whose `before` is not in its clone from every commit its head reaches**, and
  fails on a range `git rev-list` refuses. A squash is never used, because GitHub re-signs it (encri D7). (L6)
- **Publishing is to Maven Central under `com.systemhalted`**, verified by a DNS record on `systemhalted.com`; not
  GitHub Packages, which needs a token to read, and not JitPack, which builds the AAR on its own machines. Nothing is
  published. Licensed Apache-2.0 (encri D115, flagged there). (L1)

## Traps already paid for
- With a warm build cache the Java compile came back `FROM-CACHE`, and the aidl tool's Java never reached CodeQL's
  database. (L7)
- Under Kotlin 2.2 CodeQL captured Kotlin without the in-process compiler; under 2.4.21 a planted file compiled and
  never reached the database. (L7, L9)
- A metadata file regenerated over the old one still held all 31 components of the Kotlin 2.2.10 toolchain, which a
  scan then reported as though the build ran them. (L9)
- The first metadata missed `aapt2`, which only an executing resource compile asks for, and the next clean build failed
  verification on it. (`docs/conventions.md`)
- A force push's `before` is not in the runner's clone, and `commit-policy` checked no commit and passed; `set -e` did
  not reach a `git rev-list` inside a `for` list. Found before the first push. (L6, `CHANGELOG.md`)
- `--add-opens` for `java.io`, the first guess for SDK 36, changes nothing. (L8)
- `abiValidation {}` registers its tasks on an AGP library and its dump writes nothing: KGP 2.4.21 does not wire AGP
  9.4.1's compilations in. (L16)
- Non-provider secret patterns and validity checks are paid features even on a public repository; asking for them
  left them off without an error. (L7)

## Measured, not assumed
- CodeQL 2.27.3, the pinned action's: a planted `Cipher.getInstance("DES")` in Kotlin and a planted
  `pull_request_target` injection were both found; the code and the workflows had no findings. (L7)
- Strict locking refuses a missing lockfile; a planted `commons-lang3` 3.16.0 fails the scan of what ships; removing
  freemarker's triage entry brings its advisory back. (L10)
- A failing test fails `allUnitTests`; one untested Kotlin file fails `koverVerify` at 0% against 90%; without L4's
  filter `encri-contract` fails at 0%. A rule over no code passes. (L4, L8)
- A class file's `kotlin.Metadata` went from `mv=[2,2,0]` to `mv=[2,4,0]` with Kotlin 2.4.21, and back to a 2.2 floor
  checked with `kotlinc` 2.1.21 and 2.0.21. (L9, L17)
- Every artifact in the metadata has encri's hash; one, `annotation-jvm-1.7.0.jar`, is not in encri's file. (L13)

## Decisions
| | Why it matters here |
|---|---|
| L3 | encri's toolchain and CI, and what was left out |
| L4 | Coverage gates, diff coverage, the generated-code filter |
| L7 | CodeQL over code and workflows; secret scanning; flagged on the query suite |
| L9 | Kotlin 2.4.21; metadata from empty; CodeQL in-process |
| L10 | Dependency scanning: what ships is a gate, what builds it a report; lockfiles |
| L16 | `:api-dump` and the dumps; Kotlin's own ABI validation rejected for now |
| L17 | The Kotlin floor for apps; flagged |
| L18 | The append-only check in CI |
| L8 | Robolectric, the aggregate's wiring measured, the SDK 36 flag; two veto flags |
| L13 | The device tests' artifacts in the metadata |
| L1 | Coordinates, minSdk, explicit API, Maven Central |
| L6, L19 | The repository's name; the first push; history not rewritten (flagged) |
| L26 | This documentation's layout; `.claude/rules/` committed (flagged) |
| encri D7, D11, D115, D116, D119, D126 | Pinned actions and no squash; the aggregate; the licence; reproducible releases; Kotlin; encri's pin of this repository |

## Open here
- When the repository is pushed (`ROADMAP.md`): branch protection with force pushes refused again, merges without
  squash, the setting that refuses an unpinned action, and whether the first CodeQL baseline lists the aidl tool's Java.
- `gradle/osv-scanner.toml`'s entries expire on 2027-01-22; each is renewed only if its reason still holds.
- Publishing: the `com.systemhalted` namespace verified on Maven Central, how artifacts are signed (an open question),
  and reproducible AARs checked as encri's release is (encri D116). (L1)
- The reproducible-build check L3 left out has nothing to check until a release.

## Read next
- Related briefs: `.claude/rules/device-testing.md`, `.claude/rules/contract.md`
- `docs/conventions.md` in full; encri's: `../encri-core/.claude/rules/build-ci-release.md`
- Full records: `docs/decisions/INDEX.md`
