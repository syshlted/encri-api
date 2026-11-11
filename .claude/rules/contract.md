---
paths:
  - "encri-contract/**"
  - "PROTOCOL.md"
  - "scripts/ci/append-only.py"
---

# Contract — component brief

The wire: the `.aidl` files in `encri-contract` (`IEncriService`, `IEncriSession`, the three oneway listeners and the
structured parcelables), the tests that pin them (`WireNamesTest`, `ParcelableEvolutionTest`,
`UnansweredTransactionTest`), the append-only check `scripts/ci/append-only.py`, and `PROTOCOL.md`, the written
specification for an author writing a client of their own. About the contract this repository's records win, and
encri's service follows them (encri D115). This brief is level 1; full records are in `docs/decisions/`.

## Rules that hold here
- **The package and each method's position are wire names.** A transaction carries its interface's full name,
  `com.systemhalted.encri.api.IEncriService`, as its token, and the aidl tool numbers methods by position. Moving a
  file to another package, or removing, reordering or inserting a method, breaks every installed client. Permanent
  from the first `v*` tag; before it the names could change, and did. (L2, L19)
- **The contract only grows.** Methods and a parcelable's fields are only appended; every constant keeps its value;
  `VERSION` may only rise; nothing is removed or changes kind, `oneway` included. `append-only.py` checks it in CI
  against the newest `v*` tag, and says there is nothing to compare until one exists. (L2, L11, L18)
- **Transaction 1 is `int contractVersion()` and takes nothing.** `VERSION` is a constant in `IEncriService.aidl`,
  because the file is the specification. It is 2: version 2 brings `handOffNamed`. (L2, L24)
- **A method an older service lacks answers 0, `false` or `null`, never an error.** So a client never calls above
  `contractVersion()`, version 0 is never encri, and the line starts at 1. (L2, L8, L13)
- **Every result is a parcelable with a status, and no status is 0.** `STATUS_OK` is 1, a null result means *not
  answered*, and an operation with nothing to return gives an `Outcome`, never an `int`. A new method should answer
  something whose default is not a success; nothing checks that. (L2, L11)
- **Two steps, and the second is a capability.** `openSession(IVaultListener)` hands one app an `IEncriSession`; encri
  checks the caller's uid on every call, and the session dies with the app's process. A locked vault answers
  `STATUS_LOCKED` to every app at `openSession`. (L11)
- **The vocabulary is constants in `IEncriService`**: the package, the bind, consent and unlock actions, the two
  backup screens' actions, rights, states, statuses, `FLAG_FREE_NAME` and the folder MIME type. The contract's
  actions are under `com.systemhalted.encri.api.action.`; `ACTION_UNLOCK` keeps `com.systemhalted.encri.action.`
  because it predates the contract (encri D33). A client binds by action with the package set, never by class
  name. (L11, L19, L20)
- **Rights are five bits, and every grant carries read.** A grant is rooted at the whole vault or at one folder; an
  id outside it answers `STATUS_NOT_FOUND`, like an id that does not exist; the root's parent reads as null. (L11)
- **Writes go over a reliable pipe; reads are proxy descriptors.** A clean close commits; `closeWithError` or a dead
  writer abandons and leaves the document as it was; `IWriteListener` says which. A write cannot seek, so it never
  leaves a hole. Do not move writes to a proxy descriptor: its commit has no errno, and AppFuse wedges on large
  writes (encri D26, D87, D88). (L11)
- **What takes time is answered on a oneway listener**, so no call holds a binder thread the file picker also needs:
  the end of a write, and a preview, which is queued with a ticket, cancellable, and an encoded image no larger than
  the size asked on either side. The promise stands; encri was changed to keep it (encri D121). (L11, L13)
- **Listing is paged in name order with an opaque cursor, and encri caps the page**, because Binder's buffer is about
  1 MB and shared (encri's hard constraint 4). (L11)
- **What the user allows replaces the grant** with exactly the rights asked for, and ends every session the app holds;
  a refusal or a back-out changes nothing. **A grant whose folder is deleted ends its sessions.** (L23)
- **A handoff's name.** From version 2 a plain `handOff` gives the receiving app a made-up name, twelve hex digits and
  an extension from the type; `handOffNamed`, appended after `end()`, gives the real one. A client asks for it only on
  the user's tick for that handoff, on a share and an *open with* alike. Which app receives it plays no part.
  (L24, L25, encri D147, D148)
- **Backup files and restores are encri's screens**, named by `ACTION_SAVE_BACKUP_FILE` and `ACTION_RESTORE`, started
  with `startActivity` and no session. Their passphrases are typed on encri's screen only. A constant does not raise
  `VERSION`. (L20, L21, encri D123)
- **A failing `WireNamesTest` is fixed by putting the contract back**, never by changing what it expects: each value
  is one an installed client compiled in. It pins every interface token, every transaction code and the version.
  (L2, L11, L24)
- **The aidl tool's classes are excluded from coverage by name**: every class an `.aidl` file names, with its inner
  classes. Without the filter `encri-contract` fails at 0% against its 90% line gate. (L4, L11)
- **The contract is not in the API dump**; its Java is the aidl tool's, and its wire is guarded above. (L16)
- **`PROTOCOL.md` is for authors who declare `encri-contract` and write their own client**; it changes with every
  method and promise. (L5, L23, L24)

## Traps already paid for
- L2's zero was read from the generated proxy and never run until L8. The proxy assigns `transact`'s return to
  `_status` and never reads it, so a build-tools release that turns an unanswered transaction into an exception
  fails `UnansweredTransactionTest`, and L2's consequences are re-read rather than quietly changing. (L8)
- The mockable `android.jar` has no `Parcel`, so the contract's tests run under Robolectric. API 27's
  `Binder.queryLocalInterface` dereferences a descriptor a bare `Binder` never attached, so the test's binder answers
  null, as a `BinderProxy` does. (L8)
- The first coverage filter, `inheritedFrom("android.os.IInterface")`, let the structured parcelables through at 13.9%
  against 90%. A filter on `Parcelable` would also sweep out a hand-written one. (L4, L11)
- encri's companion path reused the file picker's *at least the hint* rule and answered a 64x64 preview at 150x100.
  The wording did not follow encri: the promise is what lets a client bound a grid's cost. (L13, encri D121)
- `int` returns with `out` parcelables read as success from an older encri. Rejected. (L11)
- A flag added to `handOff` in place, or an `int` of options, was rejected: version 1 is in apps already, and an
  option bit needs an answer for every bit a service does not know. A second option is a second method. (L24)
- `ACTION_BACKUPS` named a screen that left encri; it was removed before the freeze, because an action that resolves
  to nothing could never be taken back once released. (L21)
- The rename to encri broke clients built before it, which was acceptable only because nothing was released. After a
  tag the same change fails `WireNamesTest`, the dumps and the append-only check. (L19)

## Measured, not assumed
- A binder that does not know transaction 1 answers 0 with no exception, beside a control answering `VERSION`: under
  Robolectric at API 27 and 36 (L8), and over a real `BinderProxy` on the G6, the S9, the A20 and the API 27 emulator
  (L13).
- `ParcelableEvolutionTest` runs both halves: a reader skips appended fields and defaults missing ones. (L11)
- `append-only.py` accepts the contract before L11 and names each planted breach: two methods swapped, a field
  retyped, a status renumbered, `VERSION` lowered, `oneway` dropped. (L18)
- R8 renamed the contract's classes in a minified app and nothing broke against encri on the A20: every `CREATOR` is
  reached statically and a transaction carries its interface's name as a literal. (L18)
- Asked again from an open session, five ways, on the G6 and the A20 (two builds of encri 0.1.0), encri answered as
  L23's table says; a deleted root left its session open and told it nothing (encri-images I9, I12). (L23)

## Decisions
| | Why it matters here |
|---|---|
| L2 | Names and positions are permanent; transaction 1; the version; unanswered methods read as zero |
| L11 | Contract v1: sessions, statuses, rights, pipes, listeners, paging; three veto flags |
| L18 | The append-only check; no consumer R8 rules |
| L19 | Every wire name says encri, permanent from the first tag; one veto flag |
| L23 | What a consent and a deleted folder do to a session; two veto flags |
| L24, L25 | `handOffNamed` and `VERSION` 2; the user's tick on a share and an *open with*; three veto flags |
| L20, L21 | encri's backup screens by action, with no session; `ACTION_BACKUPS` removed (flagged) |
| L8 | The zero run under Robolectric at API 27 and 36; two veto flags |
| L13 | The zero over a real Binder; the preview promise kept against encri |
| L4 | Coverage excludes what the aidl tool generated, by name |
| L5 | The contract is published for authors of their own client |
| L1 | One copy of the wire, which encri's service compiles against |
| L14 | A shared-memory read path would be one more appended method |
| L16 | The contract is not dumped |
| encri D113–D118, D121, D123, D137, D147, D148 | What crosses the boundary and why; encri's side of each promise |

## Open here
- `VERSION` 2 before the first `v*` tag, while apps speak version 1 (L24, flagged).
  Not amended.
- `PROTOCOL.md` still says *This document describes version 1*, and describes `handOffNamed`, which is version 2. Not
  corrected.
- L23's second promise is ahead of encri, which leaves the session open: encri B143.
- The backup app's surface (encri D137, D146; encri-backup K1) is undesigned. It is designed here as a record when it
  is, and adds to the contract (`ROADMAP.md`, *Now*). L21's text still names D137's two backup apps, which encri D146
  made one.
- The contract freezes at the first release, which is encri's and Encri Files' too (encri D136).

## Read next
- Related briefs: `.claude/rules/client.md`, `.claude/rules/device-testing.md`
- `PROTOCOL.md`; encri's side: `../encri-core/.claude/rules/companion-api.md`
- Full records: `docs/decisions/INDEX.md`
