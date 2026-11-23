// The backup app's session (L27), from IEncriService.openBackup. Its methods are only ever appended,
// as IEncriSession's are. It answers the app the vault's backup was set up with, locked or not, and
// gives it nothing a document holds: it runs backups into the folder the app serves.
package com.systemhalted.encri.api;

import com.systemhalted.encri.api.BackupStateResult;
import com.systemhalted.encri.api.IBackupFolder;
import com.systemhalted.encri.api.Outcome;

interface IEncriBackup {
    /** Whether a backup is set up, whether the vault is locked, and whether a stopped run waits. */
    BackupStateResult state();

    /**
     * Backs up into folder now, keeping keep generations (1 to 1000). Unlocked, a whole run; locked,
     * the rest of a run that stopped part way, or STATUS_LOCKED when none waits. Answers STATUS_OK as
     * the run starts, STATUS_BUSY while another goes, or STATUS_INVALID for a keep out of range; the
     * run's end comes once on the session's listener. folder is called until then and not after.
     */
    Outcome run(IBackupFolder folder, int keep);

    /** Asks the run that is going to stop between copies. Its end comes on the listener as ever. */
    oneway void stop();

    /** Stops backups: encri forgets this backup and this app. Nothing in the folder is touched. */
    Outcome stopBackups();

    /** Ends the session, stopping its run. */
    oneway void end();
}
