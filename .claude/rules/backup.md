---
paths:
  - "encri-contract/src/main/aidl/**/*Backup*.aidl"
  - "encri-contract/src/main/aidl/**/FolderPage.aidl"
  - "encri-client/src/**/*Backup*.kt"
---

# The backup surface — component brief

What the contract gives Encri Backup, `com.systemhalted.encri.backup`: the backup session `IEncriBackup`, the folder
the app serves `IBackupFolder`, the run's listener `IBackupListener` and their parcelables; `ACTION_SET_UP_BACKUP`
and `ACTION_VAULT_UNLOCKED`; and the client's `EncriBackup` and `BackupFolder`. The design is L27. encri's half is
its D149, and the app's is encri-backup's K4.

## Rules that hold here

- **The app serves a backup store, and encri runs the backup.** `IBackupFolder`'s four methods are `BackupStore`'s
  five operations (encri's `core-export`), and nothing more. A method that would let encri ask the app anything
  else, or the app ask encri for a byte of the vault, does not belong here.
- **Only the backup's own files cross.** A listing names files whose names begin `encri-` and use `[0-9A-Za-z._-]`,
  at most 128 characters, and never a folder. The app refuses every other name with `STATUS_INVALID`.
- **A listing still loading is never served** (encri B73). The app asks the provider again and then answers
  `STATUS_FAILED`, because encri's pruner deletes on what a listing names.
- **Descriptors, never bytes.** `openRead` and `openWrite` hand over the provider's own descriptor. No folder byte
  is in a parcel (hard constraint 4). A listing is paged, at most 1,000 names a page.
- **`run` answers at once, and its end comes on the listener**, once. Progress arrives at least every 30 seconds
  while a run works. The client gives up after two minutes of silence, ending the session, and a session that ends
  stops its run.
- **The backup session opens locked** (L11 amended). It answers in this order: `STATUS_ACCESS_OFF`, `STATUS_INVALID`
  for a null listener, then `STATUS_NOT_AUTHORIZED` unless the caller is the app the backup was set up with.
- **The set-up screen carries the folder as a binder.** An intent can carry a binder and cannot carry a descriptor.
  The passphrase is typed on encri's screen, never sent (encri D78, D123).
- **The client's folder stub answers encri's uid alone**, and hides every AIDL type behind `BackupFolder` (L5).

## Traps already paid for

- **A run's end can arrive before `run`'s answer**, because the listener is oneway. The client starts listening
  before it asks; a case fails with the order reversed.
- **A set-up Intent sent to a lookalike would hand it the folder.** `Encri.setUpBackupIntent` answers null unless
  encri is installed and trusted, and the folder's stub answers encri's uid alone.

## Measured, not assumed

None yet. Unmeasured: the app's job lending encri its importance through the binding for the length of a run, and
whether a phone that kills encri in the background (the A20, encri-firefox's `open-questions.md`) kills it mid-run.

## Decisions

| | Why it matters here |
|---|---|
| L27 | The whole surface |
| L5, L11, L12 | The client hides the contract; results, listeners and sessions |
| L15 | The registry's `backup` right |
| L18 | The surface only grows once released |
| L21 | `ACTION_BACKUPS` stays removed |

## Open here

- Whether `onRunProgress`'s thirty seconds hold through a long write into a slow cloud provider. A copy reads in
  1 MiB slices (encri's `ContainerCopier`), so progress can be told between slices.

## Read next

- Related briefs: `.claude/rules/contract.md`, `.claude/rules/client.md`, `.claude/rules/registry.md`
- `../encri-core/docs/decisions/D149-*.md`; `../encri-backup/docs/decisions/K004-*.md` and
  `../encri-backup/.claude/rules/encri-boundary.md`
