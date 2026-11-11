---
paths:
  - "encri-client/src/main/**"
  - "encri-client/src/test/**"
  - "encri-client/api/**"
---

# Client — component brief

`encri-client`, the one artifact an app declares: `Encri` (connecting, trust, and the Intents for encri's screens),
`EncriConnection`, `EncriSession`, `EncriTrust`, the result and value types in `Types.kt`, and `internal/` (the
contract's names, the bind, package identity, the session's listener); its unit tests against `FakeEncri`; and its
public API dump, `api/encri-client.api`. Encri Files and Encri Images are its clients (encri D112, D145). This brief
is level 1; full records are in `docs/decisions/`.

## Rules that hold here
- **The client hides the contract.** It depends on `encri-contract` with `implementation`, so no AIDL type is on an
  app's compile classpath, and no public or protected signature may name `com.systemhalted.encri.api`.
  `PublicSurfaceTest` holds that, reading the client's classes from a directory or a jar. (L5, L12)
- **The public API is `api/encri-client.api`**, written through kotlin-reflect by `:api-dump`, with parameter names
  and defaults. `PublicApiTest` fails on any difference, naming the lines; `-PupdateApi` writes the new dump, so a
  change to what an app compiles against is a diff in review. Explicit API mode is on. (L1, L16)
- **Every request is a `suspend` function answering an `EncriResult`**: `Ok`, or `Failed` with a `Failure` and a
  message for a developer. Each moves its work to `Dispatchers.IO`. Nothing is thrown at the app but its own
  cancellation: not a status, not encri's death, not an ended session, and not a success that arrived without its
  payload, which is `Failure.FAILED`. (L12)
- **Coroutines are an `api` dependency**, kotlinx-coroutines 1.10.2, because `suspend` and the events' `SharedFlow` are
  in public signatures. It is locked and scanned with what ships. (L10, L12)
- **encri is checked before anything is bound.** `EncriTrust.DEFAULT` trusts an encri whose signing-certificate
  history holds a certificate in `RELEASE_CERTIFICATES`, which is encri's app signing certificate
  `BA:30:D5:EE…4C:CC:49`, or one signed by the app's own key; `alsoTrusting` pins more. The history comes from
  `SigningInfo` from API 28 and `PackageInfo.signatures` at 27, and the library compares digests itself. A certificate
  encri rotated away from is still trusted. (L12, L22, encri D114, D115)
- **The bind is by action with the package set**, never by component. The callback checks the package again; a service
  answering version 0 is not encri; one older than the client is `TooOld`; ten seconds without a connection is
  `Unavailable`. (L2, L11, L12)
- **Never call a method above encri's version.** `handOff(documentIds, withNames = true)` against an encri below
  version 2 answers `Failure.UNSUPPORTED` without asking it. (L2, L24)
- **A connection owns its sessions.** Closing it ends every session opened through it and tells encri. A session
  watches its binder: encri's death ends it, emits `VaultEvent.Ended`, and answers any request waiting on a listener
  with `Failure.DISCONNECTED`. (L12)
- **A write is told encri's reason.** It goes over the pipe in 64 KiB pieces and does not seek. A writer that fails
  with an `IOException` waits up to two seconds for encri's answer and reports it; any other failure, or a caller that
  cancels, abandons the write through the pipe's error channel, and encri saves nothing. (L11, L12)
- **Events are a `SharedFlow` that drops its oldest past 64**, because encri calls the listener oneway on a binder
  thread and a listener that blocked would hold it. A slow collector may miss a `FolderChanged`, which is best effort.
  (L12)
- **The library declares `<queries>` for encri's package**, merged into the app's manifest: from API 30 an app sees
  only the packages it declares. (L12)
- **The Intents for encri's screens carry the action and the package, and nothing else.** `authorizationIntent(rights)`
  and `unlockIntent()` are started for a result; `saveBackupFileIntent()` and `restoreIntent()` with `startActivity`,
  and need no connection or session. (L12, L20, L21)
- **An app asks for every right it means to keep.** What the user allows replaces the grant and ends the app's
  sessions, so a client waits for the session that replaces the one encri ends; `authorizationIntent` says so. (L23)
- **The callbacks that never read the `ComponentName` accept null**, because Robolectric's unbind passes none. (L13)
- **`ServiceBinding.connect` is the one seam**, taking the bind intent and the package to expect, so the device tests
  can bind a stand-in. Nothing public changes for it. (L13)
- **Main sources compile at Kotlin language and API version 2.2 and declare kotlin-stdlib 2.2.10**, so an app needs
  Kotlin 2.1. Coverage gates: 90% line, 85% branch. (L4, L17)

## Traps already paid for
- Every transform `require`d its field, and a success without its payload threw `IllegalArgumentException` at the app.
  The one place every request passes through answers it now. (L12)
- The first draft of closing a connection only unbound, and left encri holding sessions for a client that had let go
  of them. (L12)
- A client that answered a broken pipe at once reported every refusal as `CANCELLED`, because encri's reason arrives
  a moment after the pipe breaks. (L12)
- `PublicSurfaceTest` looked in a directory while AGP hands a unit test a jar, so it read nothing; its own sanity case
  failed on the first run. Once it could see, `EncriSession`'s `internal` constructor was public in bytecode and named
  `IEncriSession`. It is private now, behind a `@JvmSynthetic` factory. (L12)
- `hasSigningCertificate`, which the roadmap first named, would have made API 28+ a path no unit test reaches:
  Robolectric does not shadow it. (L12)
- A connection test failed whenever the looper ran the disconnect first: the name it passes was declared non-null.
  Found by running the suite under load. (L13)

## Measured, not assumed
- A consumer that depends on the client alone cannot name `IEncriService.VERSION` (*Unresolved reference 'api'*);
  declaring the contract too, it compiles. (L5)
- 83 tests against a fake service and session at API 27 and 36 when L12 landed, with one mutation per fix, each
  failing its test. (L12)
- Against encri on the S9 and the A20, 10 of 10; a write encri refuses gets `EPIPE` and no SIGPIPE, so the app
  survives. (L13)
- A minified app with R8 on ran every request against encri on the A20 with no consumer rules. (L18)
- `kotlinc` 2.1.21 compiles a consumer against the client; 2.0.21 refuses its 2.2 metadata. (L17)

## Decisions
| | Why it matters here |
|---|---|
| L12 | What the client is; five veto flags |
| L5 | The client hides the contract |
| L16 | The public API dump and its check |
| L22 | `RELEASE_CERTIFICATES` holds encri's release certificate; one veto flag |
| L23 | What the user allows replaces the grant: ask for every right |
| L24, L25 | `handOff(withNames)`, refused below version 2; asked on the user's tick |
| L20, L21 | The Intents for encri's backup screens; `backupsIntent()` gone |
| L11 | Bind by action; writes over a pipe; listeners |
| L2 | Never call above the version; version 0 is not encri |
| L13 | The seam for the device tests; null names |
| L17 | Kotlin 2.1 for an app, stdlib 2.2.10; flagged |
| L18 | No consumer R8 rules |
| L10 | The client's runtime classpath is locked and scanned |
| L1, L4 | Coordinates, minSdk 27, explicit API mode; coverage gates |
| L14 | A read path that skips AppFuse waits for a client that reads content itself |
| encri D114, D115 | Trust before the bind; the same key trusts the same key |

## Open here
- The README says the library trusts, by default, "the app's own key; otherwise one the app pins". Since L22 the
  default also trusts encri's release certificate. Not corrected.
- L12's five flags: coroutines in the public API, an encri signed by the app's own key trusted by default, rotated
  certificates trusted, events dropped oldest first, and the two-second wait.
- The shared-memory read path is not built; its trigger is a client that reads a document's bytes in its own process.
  (L14)
- Publishing to Maven Central waits for the namespace and a decision on signing artifacts. (L1)

## Read next
- Related briefs: `.claude/rules/contract.md`, `.claude/rules/device-testing.md`, `.claude/rules/build-ci-release.md`
- Full records: `docs/decisions/INDEX.md`
