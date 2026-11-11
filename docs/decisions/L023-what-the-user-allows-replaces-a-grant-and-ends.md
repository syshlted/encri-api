# L23 — What the user allows replaces a grant and ends its sessions, and so does deleting a grant's folder
*2026-11-03 · accepted at the user's direction ("I like your suggestions regarding the decisions"); **flagged for veto
on two points** (below). Answers two questions encri-images raised (its I9 and I11), which `PROTOCOL.md` left open.
encri owes the second.*

**`PROTOCOL.md` said how an app asks for access, and not what becomes of the grant and session it already holds.**
encri-images' probe (its I12) asked encri's consent screen again from an open session, five ways, on the G6 and on the
A20, which run two different builds of encri 0.1.0. Both answered alike:

| Asked, from what the row before left | The screen answered | The old session | The next session |
|---|---|---|---|
| R+W+C, from R+W+C+D, allowed | OK, R+W+C | heard `STATE_ENDED`, then answered `STATUS_ENDED` | R+W+C |
| R+W+C+D, allowed | OK, R+W+C+D | the same | R+W+C+D |
| R+W+C+D again, allowed | OK, R+W+C+D | the same | R+W+C+D |
| R+W+C+D+M, refused | `RESULT_CANCELED`, no rights | heard nothing; works, its grant R+W+C+D | R+W+C+D |
| R+W+C+D+M, backed out of | `RESULT_CANCELED`, no rights | the same | R+W+C+D |

**That is the contract now**: what the user allows replaces the grant with exactly the rights asked for, so asking for
fewer narrows it and nothing is merged; allowing ends every session the app holds; a refusal or a back-out changes
nothing. encri-files (its F5) and encri-images (its I11) already depend on it: each waits for the session that replaces
the one encri ends, and each asks for every right it means to keep. The client's `authorizationIntent` says so.

**A grant whose folder is deleted ends its sessions.** The folder a grant is rooted at cannot be deleted through that
grant, but another app with the whole vault can delete it. encri 0.1.0 then drops the grant, so `refreshGrant` answers
`STATUS_NOT_AUTHORIZED` and a new session is refused, but it leaves the session open and tells it nothing: no
`STATE_ENDED`, and no `onFolderChanged`, because the change is to the folder's parent, outside the grant. Its root
answers `STATUS_NOT_FOUND`, and an app that does not ask goes on showing what it last listed (encri-images I12, on the
G6; I9 saw the same on the S9). **encri is to end the session**, as it ends one whose app is turned out: to the app a
grant that is gone is a grant taken away, and every client already handles the end.

*Rejected:* writing today's behaviour into the contract, which leaves every app to notice a missing root or poll
`refreshGrant` (encri-images I7 rejected polling). *Rejected:* `onFolderChanged` for the root, which is best effort by
the contract and would leave a session open with nothing it can reach.

**Flagged for veto:**

1. **Allowing the same rights again still ends the session.** It is what encri does and the simplest rule to state; a
   client that asks needlessly pays for a new session.
2. **The contract is ahead of encri on the second promise**, as it was when L13 found a preview larger than promised
   (encri D121). encri 0.1.0 leaves the session open; until encri ends it, a client sees `STATUS_NOT_FOUND` at its root
   and `STATUS_NOT_AUTHORIZED` from `refreshGrant`.

**Verified** against encri on the phones by encri-images' probes, `GrantChanges` and `GrantGone`; `AgainstEncriTest`
checks neither yet (`ROADMAP.md`). Here only `PROTOCOL.md` and comments changed: the contract's and the client's
suites, every lint and the API dump pass as before.
