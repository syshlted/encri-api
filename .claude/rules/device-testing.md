---
paths:
  - "encri-client/src/androidTest/**"
  - "scripts/device/**"
  - ".github/workflows/instrumented.yml"
---

# Device tests and the shared-memory spike — component brief

The client over a real Binder: `CrossProcessTest` against `StandInEncri` and `OlderEncri`, `AgainstEncriTest` against
the encri installed on a phone, with `EncriScreens` and `ResultHost` to drive encri's screens; the shared-memory spike
(`SharedMemorySpikeTest`, `SpikeService`); `scripts/device/run-device-tests.sh` for phones; and `instrumented.yml`,
which runs the stand-in suite on emulators. Each service runs in a process of its own. This brief is level 1; full
records are in `docs/decisions/`.

## Rules that hold here
- **Two classes, because there are two questions.** `CrossProcessTest` binds `StandInEncri` (`:standin`) and
  `OlderEncri` (`:older`, built against an older contract), needs no encri, and is what CI runs at API 27 and 36.
  `AgainstEncriTest` runs against the encri on the phone from this test's own package: a different uid, the real
  service, the real consent screen and the vault's own AppFuse descriptors. Without encri it skips itself, which is
  what CI sees, encri being closed source (encri D116). (L13)
- **One seam, and nothing public**: `ServiceBinding.connect` takes the bind intent and the package to expect, so the
  stand-in suite then follows exactly the path an app takes. (L13)
- **On a phone, `run-device-tests.sh <serial> [encri APK]`, never `connectedDebugAndroidTest`.** The Gradle task
  uninstalls the test APK, which drops the grant encri keyed on the install, and over wireless adb its exit status
  lies. The script installs with `-r` and runs one class per `am instrument`. CI uses the Gradle task, where neither
  applies. (L13)
- **`AgainstEncriTest` is ten ordered steps sharing one session**, from trust to a deleted folder, and leaves nothing
  in the vault but the grant encri keeps for this install, so a second run needs no consent screen. (L13)
- **It types and presses only in a window whose package is encri's**, through the accessibility tree, matched on
  encri's English words with case aside, and the host starts into a cleared task. (L13)
- **The vault's passphrase comes in `ENCRI_PASSPHRASE`**, passed as an instrumentation argument, so it is in the
  phone's process list while the run lasts: a test vault only. The script refuses one it cannot quote. (L13)
- **No test-only way into encri.** A backdoor that exists in some builds is one that ships by accident. (L13)
- **A device test is checked by breaking the client**: dropping the two-second wait for encri's reason, or the death
  recipient, each fails its case on a phone. (L13)
- **A preview larger than the size asked fails step 6**, which is how encri's fix is held (encri D121). (L13)
- **The spike stands in for encri's decrypt with a copy**, so only the transport differs. AppFuse reads are each block
  once, four blocks apart and shuffled, with the warm-up on blocks the timed reads never touch (encri D22). (L14)
- **When the shared-memory read path is built**: lengths and offsets travel in the Binder message, never in the
  region; encri copies out of the region once if it ever reads from it; the region is zeroed when the session ends;
  and it is handed out only after `setProtect(PROT_READ)` has succeeded. It is an appended method. (L14)
- **The suite's dependencies are encri's versions**: `androidx.test:runner` 1.7.0 and `androidx.test.ext:junit` 1.3.0.
  (L13)

## Traps already paid for
- The first draft typed a passphrase into whatever window was in front: a failed run had left encri's unlock screen in
  the test's task, and a new start brought that task forward. (L13)
- A press through the accessibility tree is a click, not a touch, so encri's consent screen's touch filter did not see
  it, and any enabled accessibility service could press *Allow*. Filed as encri B110 and closed by encri D125;
  `UiAutomation` is a tool and still reads encri's windows. (L13)
- The different-uid check was owed for a day because it needed a person at the phone. (L13)
- On the S9, reopening `/proc/self/fd/<n>` read-write is refused; elsewhere it is allowed and reaches a new region of
  no size, which no mapping fits. (L14)
- The slow round trips on the G6 and the A20 are the phones' state, a little core woken for each call, not the code
  (encri D12, D40). (L14)

## Measured, not assumed
- `CrossProcessTest` 10 of 10 on the G6 (28), the S9 (29), the A20 (30) and the API 27 emulator. `AgainstEncriTest` 10
  of 10 on the S9 in 8.4 s and the A20 in 11.3 s, from a locked vault and no grant. (L13)
- Over a real Binder: an unknown transaction reads as 0; killing encri's process ends the session and Android rebinds;
  a refused write gets `EPIPE` and no SIGPIPE; `closeWithError` reaches the other process's `checkError`. (L13)
- An open descriptor goes on serving across a lock; the session answers `LOCKED`, and the same session works after
  the unlock. (L13)
- The client cannot write through a read-only `SharedMemory` region on the G6, the S9, the A20 or the emulators at 27
  and 36, and encri's copy checksums the same after every attempt. Zeroing the region clears the client's view.
  Every target is ashmem. (L14)
- A 64 KiB block, median of 1024: through the region 83 µs on the S9 against 1.08 ms through AppFuse, 673 µs against
  2.12 ms on the A20, 1.55 ms against 4.68 ms on the G6. With encri's decrypt, about 2.5 to 3 times faster. (L14)
- What a consent and a deleted folder do to a session was measured by encri-images' probes on the G6 and the A20, not
  by this suite. (L23)

## Decisions
| | Why it matters here |
|---|---|
| L13 | The two suites, the seam, the script, pressing encri's screens; one veto flag |
| L14 | The spike, its numbers, and the read path not built; one veto flag |
| L23 | Two promises `AgainstEncriTest` does not check yet |
| L24 | A handoff's name read both ways, owed |
| L2, L8 | The zero, under Robolectric then over a real Binder |
| L12 | The client behaviours the device tests confirm |
| L3 | The instrumented matrix, added by L13 |
| encri D22, D40, D116, D120, D121, D125 | Bench method; closed source; the service; preview size; secure windows |

## Open here
- `AgainstEncriTest` has not run on the G6, which has no encri and a secure keyguard, or on the S24.
- Whether `instrumented.yml` has run on GitHub is not recorded: `ROADMAP.md` waits for a push, and the push of
  2026-10-23 records `ci`, `commit-policy`, `codeql` and `dependencies` only.
- `AgainstEncriTest` checks neither of L23's promises yet. The deleted folder needs a second package to delete it, and
  encri to end the session (encri B143).
- `AgainstEncriTest` reading a handoff's name both ways is written and has not run; it needs an encri that has built
  D147. (L24)
- memfd-backed `SharedMemory` is unmeasured; the S24 is the phone that would say. The read path is not built (L14,
  flagged). Sequential reads, where AppFuse has readahead, were not measured.

## Read next
- Related briefs: `.claude/rules/client.md`, `.claude/rules/contract.md`
- encri's device notes: `../encri-core/.claude/rules/device-testing.md`, `../encri-core/docs/dev-environment.md`
- Full records: `docs/decisions/INDEX.md`
