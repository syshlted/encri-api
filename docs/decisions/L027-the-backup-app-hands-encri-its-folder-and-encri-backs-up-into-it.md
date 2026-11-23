# L27 — The backup app hands encri its folder, and encri backs up into it, locked or not

*2026-11-23 · accepted, and **flagged for veto on five points** (below). Designs encri-api's roadmap item 9: the
backup app's surface, which L21 took `ACTION_BACKUPS` out to wait for. encri's half is its D149, and the app's is
encri-backup's K4. Answers the six questions encri-backup's `encri-boundary` brief left open. **Amends L11**,
whose locked vault refused every app, and **L15**, whose every entry lists `read`. Contract version 3. Nothing is
built yet.*

#### What has to cross, and the one idea

Encri Backup, `com.systemhalted.encri.backup`, holds the backup folder's grant and no key (encri D137, D146;
encri-backup K1). encri holds every key and no grant. A backup run needs both: the keys to prepare, and the folder to
list, read, write and delete in. Today all of it runs in encri over `SafBackupStore`, a `BackupStore` on encri's
own `ContentResolver`, and encri's run, landing and prune are all written against `BackupStore`'s five operations.

**The backup app serves `BackupStore`'s five operations to encri, as a binder of its own, and encri runs the backup
exactly as it does now.** The app opens each file through its own grant and hands encri the provider's descriptor.
encri reads, writes and syncs those descriptors itself. Every rule the run keeps today stays where it was written and
tested: blobs before the generation, a name never written over, a listing still loading never believed, a prune that
deletes only what no retained generation names. The app never sees a byte. It hands encri descriptors, and the
bytes pass from encri to the provider.

#### The wire

**`IEncriService`, version 3**, appends one method and five constants:

| | |
|---|---|
| `BackupSessionResult openBackup(IBackupListener listener)` | method 3: the backup app's session, opened locked or not |
| `ACTION_SET_UP_BACKUP` | encri's screen that sets a backup up in the folder an extra carries |
| `EXTRA_BACKUP_FOLDER`, `EXTRA_BACKUP_FOLDER_AUTHORITY` | the folder's binder, and its provider's authority, for the set-up screen |
| `ACTION_VAULT_UNLOCKED` | the broadcast encri sends the backup app's package at each unlock |
| `STATUS_BUSY = 13` | a run is already going |

**`IEncriBackup`**, the session, in this order:

1. `BackupStateResult state()`: whether a backup is set up, whether the vault is locked, and whether a stopped run
   waits to land. No time and no number, because they are sealed under the vault's key and the app keeps its own.
2. `Outcome run(IBackupFolder folder, int keep)`: back up now. Unlocked, a whole run: prepare, copy, land, prune
   to `keep` generations, record. Locked, the copy and landing of a run that stopped part way, if one waits (encri
   D132); otherwise `STATUS_LOCKED`. It answers at once, `STATUS_OK` for started, and the run's end comes on the
   listener.
3. `oneway void stop()`: the job ended; the run stops between copies, as encri D132 stops one.
4. `Outcome stopBackups()`: the user stopped backups in the app. encri forgets the backup, and the folder is left
   as it is.
5. `oneway void end()`.

**`IBackupListener`**, oneway: `onRunProgress(int phase, long done, long total)` at least every 30 seconds while a
run works, preparing or copying, and `onRunFinished(BackupRunResult result)` once per run. The result carries an
outcome (written, unchanged, landed, stopped, wrong folder, vault changed, vault inconsistent, settings unreadable),
the generation's number and counts, what was copied and pruned, when the vault was as the generation holds it, and
whether preparing found data loss. It carries no document's name.

**`IBackupFolder`**, which the backup app implements and encri calls:

| Method | `BackupStore`'s | What the app does |
|---|---|---|
| `FolderPage list(String after, int limit)` | `list`, `modifiedUtcMillis` | lists the folder once at the first page and serves the rest from that listing, in name order |
| `DescriptorResult openRead(String name)` | `open` | opens the file for reading |
| `DescriptorResult openWrite(String name)` | `write` | creates the file, or truncates it; `STATUS_NAME_TAKEN` when the provider gave it another name |
| `Outcome delete(String name)` | `delete` | deletes it; `STATUS_NOT_FOUND` when it was not there |

Only a file is listed, never a folder, and only one whose name begins `encri-` and is a name a backup store accepts
(`[0-9A-Za-z._-]`, 128 at most). The app refuses every other name with `STATUS_INVALID`, so nothing else of the
user's folder reaches encri. **A listing the provider says is still loading is never served**: the app asks again,
and then answers `STATUS_FAILED`. The pruner deletes on what a listing names (encri B73).

#### The six questions, answered

1. **Being recognized while locked.** Setting up is the consent. encri's set-up screen names the app, checks it
   against the registry, and takes the passphrase. It keeps the app's package, certificate and first install time
   in a file outside the vault's seal, beside `vault.backup`, which a locked vault can read. `openBackup` compares
   the caller with it. The file shows that this app backs this vault up, which Android's own package list already
   shows. The version is not compared: an update keeps the right and a reinstall loses it, because nothing an update
   could change reads a document (flag 3).
2. **Hearing of an unlock.** encri sends `ACTION_VAULT_UNLOCKED`, limited to the app's package, at every unlock while
   a backup is set up. The app decides whether a run is due. Anyone can send the app that broadcast, and it can only
   make the app ask encri, which answers as it would at any other time.
3. **The folder's contents, into encri.** A binder the app implements, which encri calls. encri was always reading
   an untrusted folder: a cloud provider can serve anything, and every generation and header is authenticated
   before it is believed. What is new is waiting on another app, so every call encri makes into the folder has a
   timeout, and a dead binder is a failed run (encri D149).
4. **`BackupPruner` splits into a plan and a deletion.** It does not need to: encri plans and deletes through the
   app's `delete`, one name at a time, as it does now. Retention is `keep`, which the app passes, from 1 to 1,000.
   The thirty days an unnamed blob is kept are encri's (encri D109).
5. **`backup.prepared` holds encri-private paths.** It never leaves encri.
6. **`vault.backup` holds the folder's URI.** encri no longer knows the folder. It knows the backup by its id, which
   every run already checks against the folder's key file before writing a byte. D132's search among encri's own
   grants is replaced by the folder the app hands it.

#### Setting up

The app starts `ACTION_SET_UP_BACKUP` for a result, with its folder's binder and the provider's authority as
extras. An intent may carry a binder, as Custom Tabs' does. A descriptor cannot be carried that way, so the folder is
reached through the binder. encri's screen asks for the unlock, names the app from `getCallingPackage`, refuses a
folder in encri's own provider (encri B81), and asks for a new passphrase or, when the folder holds a backup, the one
that opens it. Then it sets the backup up exactly as it does now (encri D95). The result is `RESULT_OK` with
`EXTRA_BACKUP_SET_UP` as created or reconnected, or `RESULT_CANCELED`. A restore, a check and the list of versions
stay on encri's own screens, from a folder picked there (encri D146).

#### The registry

**The registry gains the right word `backup`** (L15). A backup app's entry lists `rights: backup` and no document
right, and the set-up screen calls the app reviewed when its entry, certificate and version match, as the consent
screen does. `read` stays required of an entry that lists any document right, so no companion's entry changes.

#### The client

`EncriConnection.openBackup()` answers an `EncriBackup`. `EncriBackup.run(folder, keep)` takes a `BackupFolder`, a
Kotlin interface the app implements, and suspends until the run's end, giving up when the listener has been silent
for two minutes. Its `progress` is a flow. `Encri.setUpBackupIntent(folder, authority)` and
`Encri.backupSetUpResult(resultCode, data)` wrap the screen. The client's `IBackupFolder` stub answers only encri's
uid. No AIDL type reaches the app (L5).

#### What it costs

- **encri calls into another app during a run.** A hung app holds one of encri's threads until it answers or dies,
  past the timeout that ends the run, because a binder call cannot be interrupted. One run waits at a time, so it is
  one thread.
- **Both processes have to live for the length of a run.** The app's job holds the binding, which lends encri its
  importance. That is unmeasured on any phone.
- **A run's bytes now cross into a descriptor another app opened.** encri still holds no reference to the provider
  that served it, which is the shape encri B100's fix gave encri's own reads. A provider that dies fails the run and
  kills no one but the app holding the grant.

#### Flagged for veto

1. **encri writes into the folder, and the app does not copy.** encri D137 and the brief described the app pulling
   blob streams out of encri and writing them itself. In both shapes the app receives no key and no plaintext. In
   this one it receives nothing at all, and encri's tested run is not taken apart.
2. **encri calls an app's binder.** The alternative, encri planning from a record of what the app said had landed,
   cannot see a blob the provider damaged or another vault pruned, and encri B91 and B104 are those.
3. **An update keeps the backup right.** A companion's grant lapses at an update the registry has not reviewed
   (encri D114, D120). A backup app can read nothing, so a malicious update could take only what a cloud provider
   already holds. Lapsing would stop every user's backups at each update until they typed the passphrase again.
4. **The unlock broadcast tells the app when the vault opens.** Only the app the user set backups up with is told,
   and only while they are set up.
5. **The companion switch stops backups too.** With other apps' access off, `openBackup` answers
   `STATUS_ACCESS_OFF`, as `openSession` does. One switch turns every app away.

#### Rejected

- **The app pulls blob streams out of encri and writes them** (encri D137's sketch). The landing, its checks and the
  prune would be rewritten across two processes, and the defects live in those seams.
- **Descriptors handed over up front**, the folder's files passed in with the call. Preparing reads blob headers on
  demand, one per document, so the set cannot be known before it starts.
- **A document right for the backup app**, `RIGHT_BACKUP` among the five. Grants live in the encrypted catalog, so a
  locked vault could not read one, and a right that gives no document does not belong among the rights that do.
- **The app holding encri's set-up in its own screen**, sending the passphrase over. A passphrase is typed on
  encri's screens alone (encri D78, D123).
- **A content-URI trigger for the unlock** (`JobInfo.TriggerContentUri`). Whether the system lets a job observe
  another app's provider has not been measured.
