package com.systemhalted.encri.client

/** What [EncriConnection.openBackup] found (encri-api L27). */
public sealed interface BackupOutcome {
    public data class Opened(
        val backup: EncriBackup,
    ) : BackupOutcome

    /**
     * No backup is set up, or it is set up with another app: start [Encri.setUpBackupIntent]. A vault that is
     * locked still answers this, because the backup app is recognized without the vault's key.
     */
    public data object NotSetUp : BackupOutcome

    /** The user turned access for other apps off in encri's settings. */
    public data object AccessOff : BackupOutcome

    /** The installed encri predates the backup session: it implements [serviceVersion]. */
    public data class TooOld(
        val serviceVersion: Int,
    ) : BackupOutcome

    public data class Failed(
        val failure: Failure,
        val message: String?,
    ) : BackupOutcome
}

/** The vault's backup as a locked or open vault can say. */
public data class BackupState(
    /** A backup is set up, with this app. */
    val isSetUp: Boolean,
    val isLocked: Boolean,
    /** A run stopped part way and waits to land, which it can locked or not. */
    val hasRunWaiting: Boolean,
)

/** How far a run has got, as [EncriBackup.run] reports it. */
public data class BackupProgress(
    val phase: Phase,
    /** Documents prepared, or bytes copied. */
    val done: Long,
    /** Of how many, or null when that is not known. */
    val total: Long?,
) {
    public enum class Phase {
        /** Planning and sealing what the folder lacks, in documents. Needs the vault open. */
        PREPARING,

        /** Writing into the folder, in bytes. Carries on if the vault locks. */
        COPYING,
    }
}

/** What a run that came to an end did. A run that did not come to one is a [Failure]. */
public data class BackupRun(
    val outcome: Outcome,
    /** The generation written, landed or found unchanged, or null. */
    val generation: Long?,
    val documentCount: Int?,
    val contentBytes: Long?,
    /** Documents whose files this run copied, because the folder lacked them. */
    val documentsCopied: Int,
    val bytesCopied: Long,
    val generationsPruned: Int,
    /** When the vault was as the generation holds it, or null. A run carried on later is older than its end. */
    val asOfMillis: Long?,
    /** The run found a document damaged or missing. Which one is for encri's screens to say. */
    val vaultHasDataLoss: Boolean,
) {
    public enum class Outcome {
        /** A generation landed, of the vault as it is now. */
        WRITTEN,

        /** Nothing changed since the newest generation. */
        UNCHANGED,

        /** A run that had stopped part way was carried on, and its generation landed. */
        LANDED,

        /** Told to stop part way. What it copied is kept, and the next run carries it on. */
        STOPPED,

        /** The folder does not hold the backup this vault was set up with. Nothing was written: set up again. */
        WRONG_FOLDER,

        /** A document changed under the run. Nothing is wrong, and the next run copies it. */
        VAULT_CHANGED,

        /** Something in the vault disagrees with itself; encri's own screens say what. */
        VAULT_INCONSISTENT,

        /** The backup's settings will not open under the vault's key: set the backup up again. */
        SETTINGS_UNREADABLE,
    }
}

/** What encri's set-up screen did, from [Encri.backupSetUpResult]. */
public enum class BackupSetUp {
    /** A new backup: the folder held none. */
    CREATED,

    /** The folder held a backup, and its passphrase opened it; runs carry it on. */
    RECONNECTED,
}
