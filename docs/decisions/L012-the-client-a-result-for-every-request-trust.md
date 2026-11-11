# L12 — The client: a result for every request, trust before the bind, and a write that is told why it stopped
*2026-10-23 · accepted; **flagged for veto on five points** (below). The user chose coroutines over callbacks.
**Amends `ROADMAP.md` item 3**, which named `hasSigningCertificate` and a bind to encri's explicit component.*

**Every request is a `suspend` function that answers a `EncriResult`**: `Ok` with a value, or `Failed` with a
`Failure` and a message for a developer. Nothing a request meets is thrown at the app — not a status encri sent,
not encri's process going away mid-call, not a session that ended, and not a success that arrived without what it
should carry. That last one was thrown until a test asked: every transform `require`d its field, and an
`IllegalArgumentException` went straight to the app. The one place every request passes through now answers it as
`Failure.FAILED`. The single exception that does reach the app is its own cancellation, which structured
concurrency needs. Each request moves its work to `Dispatchers.IO`, so any of them may be called from the main
thread.

**Coroutines are an `api` dependency**, because `suspend` and the session's `SharedFlow` of events are in public
signatures. kotlinx-coroutines 1.10.2 is the client's first runtime dependency besides the standard library, so it
is in the lockfile and L10 scans it.

**encri is checked before anything is bound** (encri D114). An app trusts an encri whose signing-certificate
history holds a certificate it pinned, or one signed by its own key. The second is on by default, and it is how
encri's file manager trusts encri without naming a key, debug builds included (encri D115). The history is
read from `SigningInfo` from API 28 and from `PackageInfo.signatures` below it, and the library compares digests
itself. `hasSigningCertificate`, which the roadmap named, checks the same history, and it would have made API 28+
a second code path that no unit test can reach, because Robolectric does not shadow it.
`EncriTrust.RELEASE_CERTIFICATES` is empty until encri has a release key, so an app signed with another key pins
a certificate itself for now.

**The bind is by action with the package set**, as L11 settled, not by component. The callback checks the package
again, a service answering version 0 is not encri's (L2), and one older than this client is `TooOld`. A bind that
does not connect within ten seconds is `Unavailable`: a cold start of encri's process is the longest an honest one
takes.

**A connection owns its sessions.** Closing it ends every session opened through it and tells encri. The first
draft only unbound, which left encri holding sessions for a client that had let go of them. A session watches its
binder, so encri's process dying ends it, emits `VaultEvent.Ended`, and answers any request waiting on a listener
with `Failure.DISCONNECTED`.

**A write is told encri's reason when encri stops it.** When encri refuses a write part way — the disk is full,
the vault locked — it closes its end of the pipe, so the app's next write fails with a broken pipe, and encri's
reason reaches the write listener a moment later. A client that answered at once would report every refusal as
`CANCELLED`. So a writer that fails with an `IOException` waits up to two seconds for encri's answer, and reports
it if there is one. A writer that fails any other way, or a caller that cancels, abandons the write with the
pipe's error channel, and encri saves nothing (L11).

**Events are a `SharedFlow` that drops its oldest event past 64**, because encri calls the listener oneway on a
binder thread and a listener that blocked would hold that thread. A slow collector can therefore miss a
`FolderChanged`, which is best effort anyway, and in principle a `Locked` or `Unlocked`, which a request answers
correctly regardless.

**L5's guard had been reading nothing.** `PublicSurfaceTest` looked for the client's classes in a directory, and
AGP hands a unit test a jar. Its own sanity case, *the scan sees the client's classes at all*, failed on the first
run, which is what it is for. It reads both now, and found three leaks once it could see. Two were not real: a
listener object handed to encri, which no app can name, and a synthetic accessor. The third was real:
`EncriSession`'s `internal` constructor is public in bytecode and named `IEncriSession`, so Java could reach the
contract through it. The constructor is private now, behind a `@JvmSynthetic` factory, which neither language can
call from outside the module.

**The library declares `<queries>` for encri's package**, which merges into the app's manifest, because from API 30
an app cannot see a package it does not declare.

**Measured off a phone**: 83 tests against a fake service and a fake session, at API 27 and 36, over the client's
90/85 coverage gates. Two mutations, one per fix above, each fail their test. **Not measured**: a real encri over
a real binder, which is roadmap item 6.

**Flagged for veto:**

1. **Coroutines in the public API**, so an app receives kotlinx-coroutines 1.10.2 and cannot go below it.
2. **An encri signed by the app's own key is trusted by default.** An app that shares encri's key shares encri's
   trust already (encri D115), and a malicious app signed with that key could not be stopped by pinning.
3. **A certificate encri rotated away from is still trusted**, as Android trusts it for an update.
4. **Events are dropped oldest first** rather than holding encri's binder thread.
5. **A writer that fails on the pipe waits up to two seconds** for encri's reason before answering.

*Rejected:* callbacks, which the user declined. *Rejected:* `hasSigningCertificate`, for the reason above.
*Rejected:* throwing `RemoteException`, `IllegalStateException` or a failure type of the library's own at the app,
which would make every call site a `try` and would put the contract's exception types in the app's sight (L5).
*Rejected:* paging as a `Flow`. `listPage` answers one page, `list` gathers them, and a `Flow` would have to decide
what a failure halfway through a folder means.

<!-- amendments: generated by scripts/docs/decision-index.py; do not edit -->

## Amended since, as recorded in `amendments.md`

- **L12 and L15's empty lists are filled by L22 (2026-10-28).** `EncriTrust.RELEASE_CERTIFICATES` holds encri's app
  signing certificate, `BA:30:D5:EE…4C:CC:49`, so an app trusts a released encri by default and not only one signed by
  its own key; and the registry lists Encri Files under that certificate.

<!-- /amendments -->
