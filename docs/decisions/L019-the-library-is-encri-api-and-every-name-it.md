# L19 — The library is encri-api, and every name it carries says encri
*2026-10-23 · accepted on the user's direction, with encri D124, which records why the suite was renamed.
**Flagged for veto on one point** (below).*

**Every name that spelled the earlier one says encri now**, because nothing has been released: contract v1 freezes
at the first tag (L2), and there is none.

| What | Now |
|---|---|
| Group and artifacts | `com.systemhalted.encri:encri-contract`, `encri-client` and `encri-registry` |
| Packages | `com.systemhalted.encri.api`, `.client`, `.registry` and `.apidump` |
| The contract | `IEncriService` and `IEncriSession`; `PACKAGE` is `com.systemhalted.encri`, and the action strings are under it |
| The client | `Encri.connect`, `EncriSession`, `EncriTrust` and `EncriResult` |
| Repositories | `encri-api`, and the file manager's `encri-files` |

**L6 called `com.systemhalted.encri.api` the one name here that can never change.** That is true from the first
tag; before it, the package could change, and has.

**A client built before this cannot reach an encri built after it, nor the reverse.** An interface's descriptor is
its fully qualified name, and the service is bound by an action under the earlier package, so both changed on the
wire. After a release that would be the break L2 forbids. Before one, the only clients are this repository's tests
and the throwaway app L18 built.

**`WireNamesTest` and the API dumps changed with it**, which is what they exist for: after a release the same rename
would fail both, and the append-only check (L18) as well.

**Flagged for veto:**

1. **History is not rewritten.** This repository's has been public since its first push (L6) and carries the earlier
   name in every commit before this one. Removing it means another force push and signing every commit again;
   encri D124 records the same choice for encri.

**Verified.** 120 unit tests, the registry's validator, every lint and coverage gate, and both APKs; the API dumps
were regenerated and then checked. encri builds against this repository as a composite build, with its own suite
green.
