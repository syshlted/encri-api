# L11 — Contract v1: a session binder per app, results that are never 0, and writes over a pipe
*2026-10-22 · accepted; **flagged for veto on three points** (below). The user chose the surface: browse and
read, write operations, move, previews, the open-with and share handoff, and modification times and sizes.*

**Two steps, and the second is a capability.** `IEncriService` has two methods: `contractVersion()` and
`openSession(IVaultListener)`. A session is an `IEncriSession` binder encri hands to one app (encri D114). Every
call on it checks that the caller's uid is the uid it was opened for, so an app cannot pass its session to
another, and the session dies with the app's process. A locked vault cannot say which apps it allows, so
`openSession` answers `STATUS_LOCKED` to every app and hands out nothing.

**Every result is a parcelable carrying a status, and no status is 0.** L2 found that a method an older encri
lacks answers 0 or null. A null result therefore means *not answered*, and `STATUS_OK` is 1. A bare `int` would
read 0 for *not answered*, so even the operations with nothing to return give back an `Outcome`, never an `int`.

**A parcelable's fields are only ever appended**, as methods are. The aidl tool writes a structured parcelable's
length before its fields: a reader skips what a newer writer appended and finds what an older writer did not send
at its default. `ParcelableEvolutionTest` runs both halves.

**The vocabulary is constants in `IEncriService`**: the package, the bind action, the consent and unlock actions
(and, since L20, the actions of encri's three backup screens), the rights, the states, the statuses, one flag, and the folder MIME type. A client binds by action with the package
set, rather than by a class name, so encri can move the service without breaking a client. `ACTION_UNLOCK` is
the action encri's unlock screen has answered since encri D33.

**The rights are five bits**: read, write (replace content, rename, set times), create, delete and move. Every
grant carries read: listing, previews and handoffs all need it.

**A grant is rooted at the whole vault or at one folder.** An id outside it answers `STATUS_NOT_FOUND`, the same as
an id that does not exist, so a session cannot probe what lies beyond its folder. That is encri D13's ordering
for `openDocument`. The root's own parent reads as null.

**Writes go over a pipe, not a proxy descriptor.** `openWrite` returns the write end of
`ParcelFileDescriptor.createReliablePipe()`, and an `IWriteListener` reports whether the content was saved. Three
reasons:

1. **The commit is reported.** A proxy descriptor commits in `onRelease`, which has no errno, so a failed save
   reaches nobody (encri D26, D87). Here the listener says so.
2. **No AppFuse on the write path.** encri D88 measured a single FUSE request over ~99 KiB closing encri's whole
   mount on one ROM and wedging AppFuse for every app on another. A pipe never crosses that bridge.
3. **A crash is told from a finish.** A reliable pipe carries how its writer closed. A clean close commits;
   `closeWithError`, or a writer whose process died, abandons the write and leaves the document as it was.

It costs what a pipe cannot do: seek. A write through this path is sequential, so it never leaves a hole (encri
D63). Reads stay proxy descriptors, because they must seek, may be mapped, and may be handed to another app.

**What takes time is answered on a listener**, so no call holds one of encri's binder threads, which also serve the
file picker (encri D17, D113). That covers the end of a write and a preview. A preview is queued with a ticket,
cancellable, and delivered as an encoded image on a descriptor: a sealed `memfd` from API 30 and a proxy
descriptor below it (encri D48).

**Listing is paged by name.** Entries come in name order, which encri's `UNIQUE (parent_id, display_name)` index
already yields, and `next` is an opaque cursor. encri caps the page size, because a binder buffer is ~1 MB and
shared across the process (hard constraint 4 in encri).

**Times and sizes, as the user asked.** Every entry carries its created and modified times, a document its size,
and a folder the count of its direct children. `measure` counts everything under a folder: bytes, documents and
folders. `setTimes` sets either time, with -1 leaving it as it is, so a file manager copying a file in can keep the
file's dates.

**Flagged for veto:**

1. **Writes over a pipe**, so a companion can neither seek while writing nor leave a hole.
2. **An app may lock the vault**, for everyone. That is a file manager's lock button. The worst a granted app can
   do with it is lock the vault over and over, which costs the user an unlock and nothing else.
3. **Ids outside a grant answer *not found*.** An app that was given one folder cannot tell an id outside it from
   an id that does not exist.

*Rejected:* `int` returns with `out` parcelables, which read as success from an older encri. *Rejected:* binding by
class name, which makes the service's class permanent. *Rejected:* a proxy descriptor for writes, for the three
reasons above. *Rejected:* synchronous previews, which would let a file manager scrolling a grid of photos occupy
the binder threads the file picker also needs.
