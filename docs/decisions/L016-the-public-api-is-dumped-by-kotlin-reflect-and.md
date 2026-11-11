# L16 — The public API is dumped by kotlin-reflect and checked on every test run
*2026-10-23 · accepted. Builds the API dump `ROADMAP.md` owed before the first release, and is L5's guard grown
into a full record.*

**`api/encri-client.api` and `api/encri-registry.api` are the public API**, as an app or encri compiles against
it: every public and protected class with its kind, type parameters and supertypes, and every constructor, function
and property with its parameter names and which have defaults, sorted. `PublicApiTest` in each module compares the
module with its file; a difference fails with the lines removed and added, and `-PupdateApi` writes the new dump. A
change to what an app compiles against is then a diff in review. Checked by renaming one parameter in the client's
dump: the test named both lines.

**Read through kotlin-reflect, because the bytecode cannot tell `internal` from public**: an internal declaration is
public there, with a mangled name or, at the top level, none. The first dump of the registry listed its internal
`validate` until top-level functions were mapped back to their Kotlin declarations; the module's own check is what
said so. Parameter names are in it because a renamed parameter breaks a caller using named arguments.

**Kotlin's own ABI validation was tried first and does not work here yet.** `abiValidation {}` registers
`checkKotlinAbi` and `updateKotlinAbi` on an AGP library with the built-in Kotlin, and the dump task depends on no
compilation and writes nothing: KGP 2.4.21 does not wire AGP 9.4.1's compilations into it. *Rejected:* the
binary-compatibility-validator plugin, which brings a set of artifacts encri's verified set does not hold, for what
one unpublished module of a hundred lines does.

**`:api-dump` is that module**, shared by the tests and never published. It brings kotlin-reflect 2.4.21, test-only,
the one component the metadata gained; encri's file holds 2.4.0.

**The contract is not dumped**: its Java is the aidl tool's, and its wire is L2's to guard (L18).
