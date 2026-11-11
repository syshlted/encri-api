# L18 — The contract only grows, checked against the last release; and a consumer needs no R8 rules
*2026-10-23 · accepted. Builds the two remaining release checks `ROADMAP.md` owed that need no release.*

**`scripts/ci/append-only.py` is L2's rule as a check.** It reads the `.aidl` files at a git ref and in the tree:
every interface's methods at the ref must be a prefix of today's, unchanged; every parcelable's fields likewise;
every constant keeps its value, except `VERSION`, which may only rise; nothing is removed or changes kind, `oneway`
included. CI runs it against the newest `v*` tag, and until there is one says there is nothing to compare with.
Checked against the contract before L11, which it accepts, and against planted breaches — two methods swapped, a
field retyped, a status renumbered, `VERSION` lowered, `oneway` dropped — each of which it names. `WireNamesTest`
stays: it pins what an installed client has compiled in, and this pins what a release promised.

**A consumer needs no R8 rules, measured.** A throwaway app with R8 on, built against this repository as a composite
build, ran every request the client has against encri on the A20: connecting, the consent screen, a write and its
read back, listing, `stat`, a preview, a handoff read through `ContentResolver` and revoked, `measure`, `space`,
`setTimes`, rename and delete. R8 renamed the contract's classes — `Entry`, the session proxy — and nothing broke,
because the aidl tool reaches every `CREATOR` statically, a transaction carries its interface's name as a literal,
and the client reads nothing by name. The same run is the composite-build substitution L1 describes, from outside
this build.

*Not settled here:* publishing (L1), which needs `com.systemhalted` verified on Maven Central and the signing of
artifacts decided.
