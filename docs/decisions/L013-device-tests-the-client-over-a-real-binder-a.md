# L13 — Device tests: the client over a real Binder, a stand-in in CI and encri itself on a phone
*2026-10-23 · accepted; **flagged for veto on one point** (below). Closes `ROADMAP.md` item 6, L8's owed device
claim, and the different-uid check encri's D120 owed.*

**Two classes, because there are two questions.** `CrossProcessTest` runs the client against `StandInEncri`, a
service in a process of its own (`:standin`), and `OlderEncri`, one built against an older contract (`:older`).
It needs no encri, so it is what `instrumented.yml` runs. `AgainstEncriTest` runs it against the encri installed
on the phone, from this test's own package: a different uid, the real service, the real consent screen and the
vault's own AppFuse descriptors. Without encri it skips itself, which is what CI sees, encri being closed
source (encri D116).

**One seam, and nothing public.** `ServiceBinding.connect` takes the bind intent and the package to expect, so the
stand-in suite binds a stand-in and then follows exactly the path an app takes. The public `Encri.connect` still
binds encri's action in encri's package.

**What a real Binder adds, each measured rather than assumed:**

| Claim | Before | Now |
|---|---|---|
| A method an older encri lacks reads as 0 or null, never an error (L2) | Robolectric | over a real `BinderProxy` |
| encri's process dying ends the session and answers a request waiting on a listener (L12) | a fake death recipient | `Process.killProcess`; Android rebinds the connection by itself and a new session opens |
| A write encri refuses part way is told encri's reason (L12) | a JVM pipe | a reliable pipe across processes. The writer gets `EPIPE (Broken pipe)` and **no SIGPIPE**: the app survives |
| A writer that fails saves nothing (L11) | JVM | `closeWithError` read by the other process's `checkError` |
| A preview arrives oneway on a descriptor; listing pages; events cross oneway | fakes | across the kernel |

Two client mutations were run on the G6, one at a time, to check the device tests can fail. Dropping the
two-second wait for encri's reason fails the refused write, which then answers `CANCELLED` with `write failed:
EPIPE`. Dropping the death recipient makes the process-death case time out.

**Against encri itself**, in ten ordered steps sharing one session: trust by the same key (`checkSignatures` across
two real packages), connect, the grant, a 1 MB write read back through `/mnt/appfuse/…` with a `pread` across a block
boundary, paging and `measure`, rename, move into a subfolder, `setTimes`, a PNG's preview, a handoff read through
`ContentResolver` and refused once revoked, refusals as answers, a lock — an open descriptor goes on serving across
it, the session answers `LOCKED`, and after the unlock the same session works — and the folder deleted. It leaves
nothing in the vault but the grant encri keeps for this install, so a second run needs no consent screen.

| | API | `CrossProcessTest` | `AgainstEncriTest` |
|---|---|---|---|
| LG G6 | 28 | 10 of 10 | not run: no encri installed, and a secure keyguard |
| Galaxy S9 | 29 | 10 of 10 | **10 of 10**, 8.4 s |
| Galaxy A20 | 30 | 10 of 10 | **10 of 10**, 11.3 s, from a locked vault and no grant |
| API 27 emulator | 27 | 10 of 10 | skipped: no encri, as CI |

**It found that encri broke the contract, and the contract stands.** Asked for a preview no larger than 64x64,
encri answered 150x100. L11, `PROTOCOL.md` and the client all promise *no larger than*; encri's companion path
reused the file picker's rule, *at least the hint on both axes* (encri D50), and that rule leaves the long side
unbounded — a panorama comes back at full width. encri honours the contract now (encri D121). The wording is not
changed to match what encri did, because the promise is what lets a client bound what a grid of previews costs
without knowing how large each picture is. Reverting encri's fix fails step 6.

**How it presses encri's buttons.** Through the accessibility tree (`EncriScreens`), which `FLAG_SECURE` does not
hide. **It types and presses only in a window whose package is encri's**, after the first draft typed a passphrase
into whatever was in front: a run that failed had left encri's unlock screen in the test's task, and a new start
only brought that task forward, so the host now starts into a cleared task too. Matched on encri's English words,
case aside, because the theme draws button labels in capitals and the tree reports what is drawn.

**And a press through the tree is a click, not a touch**, so encri's consent screen's touch filter (encri D114)
does not see it. That is how this suite can press *Allow*, and it is how any accessibility service the user has
enabled could too. Filed in encri's roadmap as B110 rather than decided here: encri's screen is encri's.

**`am instrument` rather than `connectedDebugAndroidTest`** on a phone (`scripts/device/run-device-tests.sh`): the
Gradle task uninstalls the test APK when it finishes, which drops the grant encri keyed on the install, and over
wireless adb its exit status lies (encri's notes). CI uses the Gradle task, where neither applies.

**What it brought in** is `androidx.test:runner` 1.7.0 and `androidx.test.ext:junit` 1.3.0, encri's versions. The
metadata, regenerated from empty with `assembleDebugAndroidTest` and `connectedDebugAndroidTest` added, went from
353 components to 367: 14 added, none removed, all 14 in encri's file. Of its 655 artifacts every one has encri's
hash, and one, `annotation-jvm-1.7.0.jar`, is not in encri's file at all.

**A flaky unit test, found by running the suite under load.** Robolectric's unbind calls `onServiceDisconnected`
with no name, and the client declared it non-null, so a connection test failed whenever the looper ran the
disconnect before the test's own thread had finished. The three callbacks that never read the name accept null.

**Flagged for veto:**

1. **The vault's passphrase reaches the phone as an instrumentation argument**, so it is in the phone's process list
   while the run lasts. For a test vault and nothing else; the runner refuses a passphrase it cannot quote.

*Rejected:* UiAutomator, which would bring artifacts encri's verified set does not hold, for what the platform's
own `UiAutomation` already does. *Rejected:* a test-only way into encri that grants a package, which a debug build
would carry and a release build must not — a backdoor that exists in some builds is one that ships by accident.
*Rejected:* requiring a person at the phone, which is what made the different-uid check owed for a day.
