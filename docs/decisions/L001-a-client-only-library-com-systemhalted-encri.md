# L1 — A client-only library: `com.systemhalted.encri:encri-client`, with the contract beside it
*2026-10-22 · accepted; **minSdk 27 confirmed by the user the same day as a hard limit**; coordinates
revised the same day for a client-only library, and **the group settled the same day: the user owns
`systemhalted.com`***

**encri-api is client-only** — the user's direction, 2026-10-22. It is the library an app uses to
connect to encri's AIDL interface, and none of encri's service is in it; the service is encri's.

**Two modules all the same, and an app names one.** `encri-client` is what an app declares.
`encri-contract` is the `.aidl` files and the hand-written types they name; the client depends on it,
so it arrives transitively and no app names it. It is published separately because encri's service
compiles against the same files: one copy of the wire specification, which encri's build pins by
version and hash (encri D7, D116), rather than a second copy in encri kept in step by hand. The aidl
tool generates a `Stub` beside every proxy, so the contract alone could never be client-only; what makes
the library client-only is that nothing else in it serves anything.

*Rejected:* one artifact, with the contract folded into the client. AGP has no supported way to put one
library module's classes into another's AAR, and encri would then link the whole client — binding,
the check that the service is encri — to get an interface. *Rejected:* a copy of the `.aidl` files in
encri. Two copies of a wire specification is how they come to disagree.

**Named for what they are, under the product's group.** Group `com.systemhalted.encri`, so everything
first-party for encri sits under one name; artifact IDs that say encri and what they are, because a
bare `client` means nothing in an app's dependency list or in an AAR's filename. Each Gradle project is
named after its artifact, because a composite build substitutes on `group:project name`: the file
manager declares `com.systemhalted.encri:encri-client` and, while both repositories change, includes
this build instead of resolving it (encri D115). The packages do not change with them —
`com.systemhalted.encri.api` for the contract (L2), `com.systemhalted.encri.client` for the client.
Version `0.1.0-SNAPSHOT`. *First set as `com.systemhalted.libencri:contract` and `:client`, and
renamed the same day before anything depended on them.*

**They will be published to Maven Central, under a namespace the user owns.** Central verifies a
namespace before it publishes under it, by a DNS record on the matching domain, and the user owns
`systemhalted.com`, so `com.systemhalted` can be verified and the group stands. *Rejected:* GitHub
Packages, which needs a token even to read — a poor fit for a library meant to be easy to integrate —
and JitPack, which builds from source on its own machines, so the AAR an app receives is not one this
repository built and checked. Nothing is published yet, and how the artifacts are signed is open.

**Both are Android libraries.** A Binder interface is `android.*` by definition, so the purity encri
D5 keeps for `:core-*` has nothing to buy here. If something pure-JVM arrives — the registry's
validator is the likely first — it gets a module of its own, and `allUnitTests` already wires a
`kotlin.jvm` module under `test`.

**Explicit API mode in both.** A published library's surface is declared rather than inferred, which
is what encri's `PublicSurfaceTest` does for `:provider` by scanning source. Here the compiler does it:
a public class without a visibility modifier fails `compileDebugKotlin`, checked by adding one.

**minSdk 27, encri's floor, and a hard limit.** encri cannot run below 27 (encri D14), so a client
cannot find encri there; and `SharedMemory`, which encri D113 names for previews below API 30, is
itself API 27. It covers every phone here — API 28, 29, 30 and 36 — and the emulator is the only device
at 27. *Rejected:* a lower floor so that apps with an older minSdk can integrate without
`tools:overrideLibrary`. It would mean code paths on API levels where the only answer is *encri is not
installed*, and tests at levels nobody runs encri on. An app whose minSdk is below 27 raises it or
overrides the library's. **Confirmed by the user the same day.**
