# L21 — `ACTION_BACKUPS` leaves the contract before its first release
*2026-10-27 · accepted, and **flagged for veto on one point** (below). **Amends L20**, whose three actions are two.
encri's half is its D137.*

encri D137 moves the scheduled backup out of encri into two backup apps of their own,
`com.systemhalted.encri.backups_free` and `com.systemhalted.encri.backups_sub`, and encri's first build has no
automatic backups screen. `ACTION_BACKUPS` named that screen, so it goes, with `Encri.backupsIntent()`, before the
first `v*` tag makes the contract append-only (L18). `ACTION_SAVE_BACKUP_FILE` and `ACTION_RESTORE` stay: backup
files and restores stay in encri, on its screens, as L20 has them.

**Removed rather than pointed at the backup apps.** They do not exist yet; they are two packages, where each of the
client's Intents names one; and their surface, a backup app asking encri to prepare a generation, read a folder's
generations and plan a prune, is undesigned (encri D137). An action that resolves to nothing is an entry no app can
offer, and once released it could never be taken back. Adding one when the backup apps exist costs nothing, because
the contract only grows.

*Rejected:* keeping the constant and the Intent, documented as answered by nothing. Every client would resolve it
and find nothing, and it would be frozen at the first release. *Rejected:* aiming it at `backups_free`, which would
choose a tier for the user.

**Flagged for veto:**

1. **Removed, not moved**, for the reasons above. The cost is that a client written against L20 no longer compiles
   where it calls `backupsIntent()`; Encri Files never did.

**Verified.** The contract's and the client's suites, the registry's validator, every lint and coverage gate, and both
APKs. The client's API dump lost exactly `backupsIntent()`. encri builds against this commit with its own suite green,
and its new `ContractActionsTest`, which fails when any action the contract names resolves to nothing in encri, failed
on `ACTION_BACKUPS` at the previous commit and passes at this one.
