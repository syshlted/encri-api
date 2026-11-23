// The service encri exports to apps that reach the vault without SAF (encri D113). This file and
// the others in this directory are the wire specification: what is declared here is what crosses
// the boundary.
//
// Two rules make it a contract rather than an interface, and both are in AGENTS.md L2:
//
//  - The interface's name is permanent. Every transaction carries "com.systemhalted.encri.api.
//    IEncriService" as its interface token, and encri refuses one that does not match, so moving
//    this file to another package breaks every installed client.
//  - Methods are only ever appended. A method's transaction code is its position here, so removing,
//    reordering or inserting one changes what every installed client is asking for.
//
// The constants below are the protocol's vocabulary, shared by every interface here (L11). They
// are values an installed client compiles in, so a constant never changes once it has shipped.
package com.systemhalted.encri.api;

import com.systemhalted.encri.api.BackupSessionResult;
import com.systemhalted.encri.api.IBackupListener;
import com.systemhalted.encri.api.IVaultListener;
import com.systemhalted.encri.api.SessionResult;

interface IEncriService {
    /**
     * The contract version this file describes. A version adds methods after the last one and
     * changes nothing above it, so a client may call any method whose version is at most what
     * contractVersion() returns. Version 2 adds IEncriSession.handOffNamed, and from it a plain
     * handOff's URIs no longer give the receiving app the document's name (L24). Version 3 adds
     * openBackup, the backup app's session, and the backup app's screen and broadcast (L27).
     */
    const int VERSION = 3;

    // ---- Where the service is ----------------------------------------------------------------

    /** encri's package. A client binds only to a service in this package. */
    const String PACKAGE = "com.systemhalted.encri";

    /** The action a client binds with, with its package set to PACKAGE. */
    const String ACTION_BIND = "com.systemhalted.encri.api.action.BIND";

    /**
     * The Activity a client starts for a result to ask the user for access (encri D114). It
     * answers Activity.RESULT_OK with EXTRA_RIGHTS holding what the user allowed, or
     * RESULT_CANCELED. It must be started with startActivityForResult: encri reads who is asking
     * from the system, and refuses a request it cannot attribute. What the user allows replaces the
     * app's grant with exactly the rights asked for and ends its sessions; RESULT_CANCELED changes
     * nothing.
     */
    const String ACTION_AUTHORIZE = "com.systemhalted.encri.api.action.AUTHORIZE";

    /** The rights asked for, in ACTION_AUTHORIZE's request; the rights allowed, in its result. */
    const String EXTRA_RIGHTS = "com.systemhalted.encri.api.extra.RIGHTS";

    /**
     * The Activity a client starts to have the user unlock the vault. Started for a result, it
     * answers RESULT_OK once the vault is unlocked and RESULT_CANCELED if the user backs out.
     */
    const String ACTION_UNLOCK = "com.systemhalted.encri.action.UNLOCK";

    // ---- encri's backup screens (encri D123, L20, L21) ---------------------------------------
    //
    // Backup files and restores stay in encri, and a client opens encri's own screens for them. Start
    // each with startActivity and the package set to PACKAGE. They need no session and no grant; they
    // read nothing from the Intent and answer no result. The user does the work on encri's screen,
    // which asks for every passphrase itself and says what the vault's state allows. An encri that
    // predates them does not answer them, so resolve the Intent before offering the entry that starts
    // one.

    /** The screen that saves a backup file of the whole vault where the user picks (encri D78). */
    const String ACTION_SAVE_BACKUP_FILE = "com.systemhalted.encri.api.action.SAVE_BACKUP_FILE";

    /** The screen that restores a backup file into the vault, beside what is there (encri D84). */
    const String ACTION_RESTORE = "com.systemhalted.encri.api.action.RESTORE";

    // ---- The backup app (L27) ------------------------------------------------------------------
    //
    // One app backs a vault up: the one the user set backups up with on ACTION_SET_UP_BACKUP's screen.
    // It holds the backup folder's grant and serves the folder to encri as an IBackupFolder, and encri
    // runs each backup into it. It never receives a key, a passphrase or a document.

    /**
     * The Activity the backup app starts for a result to set a backup up in its folder. The Intent
     * carries the folder as EXTRA_BACKUP_FOLDER, an IBackupFolder's binder in the extras' Bundle, and
     * the folder's provider as EXTRA_BACKUP_FOLDER_AUTHORITY. encri asks for the unlock and the
     * passphrase on its own screen, names the app from the system, and refuses a folder in encri's own
     * provider. It answers RESULT_OK with EXTRA_BACKUP_SET_UP, or RESULT_CANCELED having set nothing
     * up. Setting up replaces any backup the vault had, and the app it was set up with.
     */
    const String ACTION_SET_UP_BACKUP = "com.systemhalted.encri.api.action.SET_UP_BACKUP";

    /** The folder, in ACTION_SET_UP_BACKUP's request: an IBackupFolder's binder. */
    const String EXTRA_BACKUP_FOLDER = "com.systemhalted.encri.api.extra.BACKUP_FOLDER";

    /** The authority of the provider the folder is in, in ACTION_SET_UP_BACKUP's request. */
    const String EXTRA_BACKUP_FOLDER_AUTHORITY = "com.systemhalted.encri.api.extra.BACKUP_FOLDER_AUTHORITY";

    /** What setting up did, in ACTION_SET_UP_BACKUP's result: BACKUP_CREATED or BACKUP_RECONNECTED. */
    const String EXTRA_BACKUP_SET_UP = "com.systemhalted.encri.api.extra.BACKUP_SET_UP";

    /** A new backup: the folder held none, and its key file is written. */
    const int BACKUP_CREATED = 1;
    /** The folder held a backup, and the passphrase opened it; runs carry it on. */
    const int BACKUP_RECONNECTED = 2;

    /**
     * The broadcast encri sends at each unlock to the backup app's package alone, while a backup is
     * set up: a run that needs the vault open can start now. It carries nothing, and anyone can send
     * it, so it is only ever a reason to ask encri.
     */
    const String ACTION_VAULT_UNLOCKED = "com.systemhalted.encri.api.action.VAULT_UNLOCKED";

    // What a backup run did, in BackupRunResult.outcome, when its status is STATUS_OK.

    /** A generation landed, of the vault as it is now. */
    const int RUN_WRITTEN = 1;
    /** Nothing changed since the newest generation, which the run settled as current. */
    const int RUN_UNCHANGED = 2;
    /** A run that had stopped part way was carried on, and its generation landed. */
    const int RUN_LANDED = 3;
    /** Told to stop part way; what it copied is kept, and the next run carries it on. */
    const int RUN_STOPPED = 4;
    /** The folder does not hold the backup this vault was set up with. Nothing was written. */
    const int RUN_WRONG_FOLDER = 5;
    /** A document changed under the run; nothing is wrong, and the next run copies it. */
    const int RUN_VAULT_CHANGED = 6;
    /** Something in the vault disagrees with itself; encri's own screens say what. */
    const int RUN_VAULT_INCONSISTENT = 7;
    /** The backup's settings will not open under this vault's key; set the backup up again. */
    const int RUN_SETTINGS_UNREADABLE = 8;

    // A backup run's phases, in IBackupListener.onRunProgress.

    /** Planning and sealing what the folder lacks; counts documents. */
    const int PHASE_PREPARING = 1;
    /** Writing into the folder; counts bytes. */
    const int PHASE_COPYING = 2;

    // ---- Rights a grant carries --------------------------------------------------------------

    /** List, look up, read, preview and hand off documents. Every grant carries it. */
    const int RIGHT_READ = 1;
    /** Replace a document's content, rename it and set its times. */
    const int RIGHT_WRITE = 2;
    /** Create documents and folders. */
    const int RIGHT_CREATE = 4;
    /** Delete documents and folders, a folder with everything in it. */
    const int RIGHT_DELETE = 8;
    /** Move documents and folders to another folder. */
    const int RIGHT_MOVE = 16;

    // ---- Vault states, as IVaultListener reports them ----------------------------------------

    const int STATE_LOCKED = 1;
    const int STATE_UNLOCKED = 2;
    /**
     * The session is over: the app's grant was revoked or replaced, the folder it is rooted at was
     * deleted, access was turned off, or the session was ended.
     */
    const int STATE_ENDED = 3;

    // ---- Statuses every result carries -------------------------------------------------------
    //
    // None is 0. An older encri answers a method it does not have with an empty reply, which
    // reads as status 0, so 0 must never mean done (L2).

    const int STATUS_OK = 1;
    const int STATUS_LOCKED = 2;
    /** This app has no grant, the grant does not cover what was asked, or it was revoked. */
    const int STATUS_NOT_AUTHORIZED = 3;
    /** The user turned access for other apps off in encri's settings. */
    const int STATUS_ACCESS_OFF = 4;
    /** No such document, or one outside what this app was given: the two are not told apart. */
    const int STATUS_NOT_FOUND = 5;
    /** A document of that name already exists in that folder. */
    const int STATUS_NAME_TAKEN = 6;
    /** The request itself is wrong: a bad name, a file where a folder is needed, a cycle. */
    const int STATUS_INVALID = 7;
    /** The device does not have room for it. */
    const int STATUS_NO_SPACE = 8;
    /** encri does not do this for that document, such as a preview of a type it cannot render. */
    const int STATUS_UNSUPPORTED = 9;
    /** Something went wrong inside encri; the message says what. */
    const int STATUS_FAILED = 10;
    /** The session is over; open a new one. */
    const int STATUS_ENDED = 11;
    /** The request was cancelled before it finished. */
    const int STATUS_CANCELLED = 12;
    /** Something of the kind is already going, such as a backup run; ask again when it ends. */
    const int STATUS_BUSY = 13;

    // ---- Flags ---------------------------------------------------------------------------------

    /** On a name already taken, use the first free name instead of answering STATUS_NAME_TAKEN. */
    const int FLAG_FREE_NAME = 1;

    /** The MIME type of a folder, as SAF spells it. */
    const String MIME_TYPE_FOLDER = "vnd.android.document/directory";

    /**
     * The newest contract version this service implements: the VERSION of the contract encri was
     * built against. Transaction 1, and it takes nothing, so no later version can ever need to
     * change how it is asked. A client's first call.
     */
    int contractVersion();

    /**
     * Opens a session for the calling app. Returns STATUS_OK with a session and the grant it
     * holds, or STATUS_LOCKED, STATUS_NOT_AUTHORIZED or STATUS_ACCESS_OFF with neither.
     *
     * A locked vault cannot say which apps it allows, so every app hears STATUS_LOCKED. listener
     * is told when the vault locks and unlocks and when a folder in the grant changes, for as long
     * as the session lasts.
     */
    SessionResult openSession(IVaultListener listener);

    /**
     * Opens the backup app's session (L27), version 3. Returns STATUS_OK with a session, or
     * STATUS_ACCESS_OFF, STATUS_INVALID for no listener, or STATUS_NOT_AUTHORIZED for any app but the
     * one the vault's backup was set up with. A locked vault opens it: what it can do locked is land a
     * run that stopped part way. listener hears each run's progress and end.
     */
    BackupSessionResult openBackup(IBackupListener listener);
}
