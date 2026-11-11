# encri-api — agent guide

> **Level 0 of four**, loaded into every session: only what is true for every component, laid out as the suite's
> `docs/documentation.md` says (L26, encri D127). The rest loads when you touch a component or is read when needed.
> Keep it under 200 lines. Dates: YYYY-MM-DD.

## What this is

**The library an Android app uses to reach an encri vault without SAF.** encri exports a Binder
service for apps that are not a file picker (encri D113), and this repository holds the client's
half of it:

- **the contract**: the `.aidl` files the service implements and every client calls, which are the
  wire specification;
- **the client library**: binding to encri, the check that the service bound to really is encri,
  the session encri hands out, and the consent screen a session needs (encri D114);
- **a written specification of the protocol**, `PROTOCOL.md`, for authors who do not read the decision records;
- **the registry** of recognized apps that each encri release builds in (encri D114, D115; L15).

encri's file manager, in `../encri-files`, is the first client and builds against this
library as any other app would (encri D112). Licensed Apache-2.0, which encri D115 chose and still
flags for veto.

What crosses the boundary, and why, is decided in encri's decision records — D66, D81 and D112 to D118 —
and is not restated here. What this repository owns is the contract's shape on the wire and everything on
the client's side of it.

## Relationship to the other repositories

- **Decisions here are numbered L1, L2, …** Another repository's are cited with its name: *encri D113* (and encri's
  bugs, *encri B143*), *encri-files F8*, *encri-images I3*, *encri-backup K1*. Two logs both numbered from 1 would
  make "D3" mean two things in every conversation that spans both. S1 to S4 are severities, never decisions.
- **About the contract, this repository's records win.** encri D115 moved the contract's version line here (it is
  encri D6's plugin API line), so a change to the contract is recorded here and encri's service follows it. About
  encri's internals, encri's records win, and these link rather than restate. About a client app, its own
  repository's records win.
- **The toolchain is encri's, version for version** (L3, L9). encri compiles its service against the contract and
  builds this repository from source at the commit its `config/encri-api.pin` names (encri D126), so a bump lands in
  both repositories or in neither.
- **The siblings**: `../encri-core` (encri: the vault and the service), `../encri-files` (Encri Files, the first
  client), `../encri-images` (Encri Images, encri D145) and `../encri-backup` (Encri Backup, whose part of the
  contract is still to be designed here, encri D146). In the suite superproject they sit side by side, which is what
  every `../` path assumes. Work that spans repositories is in each one's own `ROADMAP.md`, once.

## How the project's knowledge is laid out

| Level | Where | Loads | Holds |
|---|---|---|---|
| 0 | `AGENTS.md` | every session | constraints, invariants, hazards, conventions, this map |
| 1 | `.claude/rules/<component>.md` | when a file matching its `paths:` is read or edited (other tools: read it by hand) | one component's rules, traps, measurements and decisions |
| 2 | `docs/decisions/INDEX.md` | on demand | one line per decision: date, title, status, the briefs that cite it |
| 3 | `docs/decisions/Lnnn-<slug>.md` | on demand | the full decision record |

**Finding a decision.** `AGENTS.md L11` or `L11`, in code or older documents, means `docs/decisions/L011-*.md`:
every record lived in this file before 2026-11-11 (L26). Later decisions amend earlier ones in place, and a record's
footer lists the corrections `docs/decisions/amendments.md` holds.

**Elsewhere:** `ROADMAP.md` (what is next, and the bugs), `CHANGELOG.md` (what happened), `PROTOCOL.md` (the
protocol, for a client's author), and in `docs/`: `status.md` (history), `conventions.md` (in full),
`dev-environment.md` (this machine) and `decisions/open-questions.md`.

## Components (level 1)

| Brief | Covers |
|---|---|
| `contract` | `encri-contract`: the `.aidl` files, wire names, versions, statuses, the append-only check, `PROTOCOL.md` |
| `client` | `encri-client`: binding, trust, sessions, results, writes, events, the Intents, its API dump |
| `registry` | `encri-registry`, `registry/` and `FINGERPRINTS.md`: recognized apps and the keys they are signed with |
| `build-ci-release` | Gradle, the workflows, hooks, coverage, scanning, `:api-dump`, publishing and the licence |
| `device-testing` | the instrumented suites, the stand-ins, `AgainstEncriTest`, the shared-memory spike, `run-device-tests.sh` |

## Hard constraints

These are external facts that shape everything else. Not negotiable.

1. **A transaction carries its interface's full name, and the aidl tool numbers methods by position.** Moving an
   `.aidl` file to another package, or removing, reordering or inserting a method, changes what every installed
   client asks for (L2).
2. **A method an older service lacks answers 0, `false` or `null`, not an error.** The generated proxy ignores what
   `transact` returns. Run under Robolectric (L8) and over a real `BinderProxy` (L13).
3. **Binder's transaction buffer is about 1 MB, per process and shared** (encri's hard constraint 4). Bulk bytes go
   by descriptor, and listing is paged (L11).
4. **encri cannot run below API 27** (encri D14), so minSdk 27 is a hard limit, confirmed by the user (L1).
5. **A proxy descriptor commits in `onRelease`, which has no errno, and a large FUSE write can wedge AppFuse** (encri
   D26, D87, D88). Writes go over a pipe for that reason (L11).
6. **From API 30 an app sees only the packages it declares**, so the client declares encri's in `<queries>` (L12).
7. **A Kotlin compiler reads metadata one version ahead and no further** (L17), and **Kotlin lets a public signature
   name a type from an `implementation` dependency**, which then fails in the app (L5).

## Invariants

Violating any of these is a design bug, not a tradeoff.

- **The contract only grows.** Methods and parcelable fields are only appended, constants keep their values, and
  `VERSION` only rises, checked against the newest `v*` tag (L2, L11, L18). The names are permanent from the first
  tag; before it they could change (L19).
- **No status is 0.** Every result is a parcelable carrying a status, `STATUS_OK` is 1, and a null result means *not
  answered* (L2, L11).
- **A client never calls a method above encri's `contractVersion()`**, and version 0 is never encri (L2, L12, L24).
- **The client hides the contract**: no public or protected signature in `encri-client` names
  `com.systemhalted.encri.api`. `PublicSurfaceTest` and the API dump hold it (L5, L12, L16).
- **Nothing a request meets is thrown at the app** but its own cancellation (L12).
- **encri is trusted before it is bound**, by its signing certificate, and bound by action with the package set,
  never by class name (L11, L12, encri D114).
- **This library serves nothing.** It is client-only; encri's service is encri's (L1).
- **The contract is the promise, and encri follows it.** When encri answered otherwise, encri changed (L13, encri
  D121); where the contract is ahead of encri, the record says so (L23).
- **Recognition changes words, never access**, except that a version the registry refuses is not let in (L15, encri
  D122).

## Hazards

- **The repository is public**, and has been since its first push (L6, L19). What is committed is published, the
  briefs included (L26).
- **`connectedDebugAndroidTest` on a phone drops encri's grant for this install**, and over wireless adb its exit
  status lies. Use `scripts/device/run-device-tests.sh` (L13).
- **Never run encri's `:app:connectedDebugAndroidTest` on a phone that holds a vault.** It destroys the vault (encri
  D39).
- **`AgainstEncriTest` takes the vault's passphrase as an instrumentation argument**, visible in the phone's process
  list while it runs: a test vault only (L13, flagged).
- **Gradle runs on JDK 21**: set `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (`docs/dev-environment.md`). Read a
  run's results, not a piped exit code.
- **The project's earlier name is never written**: not in code, docs, commits or any repository (encri D124). The
  history before the rename carries it and is not rewritten (L19, flagged).

## What this project keeps finding

- **A check that has never failed has not been tested.** Each gate here was measured by breaking it: a planted
  cipher, a planted dependency, planted contract breaches, a reverted fix (L7, L8, L10, L13, L18). `PublicSurfaceTest`
  read nothing until its own sanity case failed (L12).
- **What was read from code is not yet run.** L2's zero was read from the generated proxy, then run under
  Robolectric, then on phones (L8, L13). A measurement true of Kotlin 2.2 was false of 2.4 (L7, L9).
- **A cache or an up-to-date task hides what it skipped**: a compile from the build cache that CodeQL never saw (L7),
  and metadata that missed `aapt2` (`docs/conventions.md`).
- **Running against the real encri finds where encri and the contract disagree** (L13, L23).

## Code rules in every module

- **Explicit API mode, and a public API that is dumped and checked** (L1, L16).
- **Main sources compile at Kotlin language and API version 2.2 and declare kotlin-stdlib 2.2.10**; tests may use
  what they like (L17).
- **Coverage gates on what was written here, never on what the aidl tool generated**: 90% line, 85% branch where it
  applies, and 85% of changed lines in a pull request (L4).
- **Prefer what encri's verified set already holds** when adding a dependency (L8, L13, L16).

## Repository conventions

Full text, with the reasons and the incident behind each rule: `docs/conventions.md`.

- **Default branch is `master`**, by SystemHalted organization policy. Never rename it to `main`.
- **Commits are signed** (key `E483D747728C1C9EA213FB29615692825276E947`) **and Conventional**, subject ≤ 72
  characters. `core.hooksPath` stays unset; lefthook runs signing preflight, ktlint and detekt on commit, and
  signatures and `allUnitTests` on push. Every rule is enforced again in CI.
- **Every commit builds and passes its own tests, not just the branch tip.**
- **Merge with a merge commit or a rebase, never a squash** (encri D7): GitHub's squash re-signs with its own key.
- **Regenerate the verification metadata from an empty file, with `--rerun-tasks`**, and the lockfiles with
  `--write-locks` (L9, L10).
- **Bootstrap a fresh clone** with `lefthook install`. The remote is `git@syshlt-github:syshlted/encri-api.git` (L6).

## Recording what is decided

1. **A settled design question becomes a decision record**, not only an answer in chat:
   `docs/decisions/Lnnn-<slug>.md`, the next free number, beginning `# Lnnn — <title>` then
   `*YYYY-MM-DD · accepted…*`, with what was rejected and measured, and *flagged for veto* where unconfirmed.
   A change to the contract is recorded here, and encri's half in encri's.
2. **Regenerate the index and footers**, `python3 scripts/docs/decision-index.py .`, never by hand; then check the
   briefs still cover the tree with `python3 scripts/docs/brief-coverage.py .`.
3. **Update the brief** of every component the decision governs: add the rule and a row to its *Decisions* table. A
   brief must stay under 12 KB; if one outgrows that, split the component.
4. **Amend earlier records in place**, with strike-through and "amended by Lnn", and add a line to
   `docs/decisions/amendments.md`. Bugs and what is next go in `ROADMAP.md`; what landed, in `CHANGELOG.md`.

**When they disagree, the decision records win**, and the briefs, `ROADMAP.md` or `CHANGELOG.md` are fixed.
