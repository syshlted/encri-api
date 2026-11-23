// What the backup app's session is told about its runs (L27). oneway, so encri never waits on it.
package com.systemhalted.encri.api;

import com.systemhalted.encri.api.BackupRunResult;

oneway interface IBackupListener {
    /**
     * How far the run has got: one of IEncriService's PHASE_ constants, how much is done and of how
     * much, -1 when that is not known. Sent at least every 30 seconds while a run works.
     */
    void onRunProgress(int phase, long done, long total);

    /** The run's end, once per run. */
    void onRunFinished(in BackupRunResult result);
}
