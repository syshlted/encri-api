package com.systemhalted.encri.api;

import com.systemhalted.encri.api.IEncriBackup;

parcelable BackupSessionResult {
    /** IEncriService's STATUS_ constants. 0 means an older encri did not answer (L2). */
    int status;
    @nullable IEncriBackup backup;
    @nullable String message;
}
