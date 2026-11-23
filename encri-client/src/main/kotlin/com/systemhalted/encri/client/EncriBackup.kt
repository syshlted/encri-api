package com.systemhalted.encri.client

import android.os.IBinder
import android.os.RemoteException
import com.systemhalted.encri.api.BackupRunResult
import com.systemhalted.encri.api.IEncriBackup
import com.systemhalted.encri.api.IEncriService
import com.systemhalted.encri.client.internal.BackupListener
import com.systemhalted.encri.client.internal.FolderStub
import com.systemhalted.encri.client.internal.RunEnd
import com.systemhalted.encri.client.internal.failureOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The backup app's session (encri-api L27), from [EncriConnection.openBackup]: it backs the vault up into
 * the folder the app serves, and gives the app nothing a document holds.
 *
 * **It opens while the vault is locked.** Locked, a [run] lands a run that stopped part way, if one waits;
 * otherwise it is [Failure.LOCKED], which is a quiet skip, not a failure. Every request answers an
 * [EncriResult], as [EncriSession]'s do.
 */
public class EncriBackup private constructor(
    private val remote: IEncriBackup,
    private val listener: BackupListener,
    private val io: CoroutineDispatcher,
    private val encriUid: Int,
    private val onClosed: (EncriBackup) -> Unit,
) : AutoCloseable {
    private val ended = AtomicBoolean(false)
    private val running = AtomicBoolean(false)

    private val death =
        IBinder.DeathRecipient {
            ended.set(true)
            onClosed(this)
            listener.gone()
        }

    init {
        if (runCatching { remote.asBinder().linkToDeath(death, 0) }.isFailure) ended.set(true)
    }

    /** Whether the session can still be used: not once it is closed or encri's process dies. */
    public val isOpen: Boolean get() = !ended.get()

    /** Whether a backup is set up, whether the vault is locked, and whether a stopped run waits to land. */
    @Suppress("ReturnCount", "SwallowedException") // One per way a request ends.
    public suspend fun state(): EncriResult<BackupState> =
        withContext(io) {
            if (ended.get()) return@withContext EncriResult.Failed(Failure.ENDED, ENDED_HERE)
            val answer =
                try {
                    remote.state()
                } catch (gone: RemoteException) {
                    return@withContext disconnected()
                } ?: return@withContext EncriResult.Failed(Failure.FAILED, NO_ANSWER)
            if (answer.status != IEncriService.STATUS_OK) return@withContext failed(answer.status, answer.message)
            EncriResult.Ok(BackupState(answer.setUp, answer.locked, answer.waiting))
        }

    /**
     * Backs up into [folder] now, keeping [keep] generations, and answers when the run ends. [onProgress]
     * hears how far it has got, on this request's own thread.
     *
     * **A run that encri says nothing about for two minutes is given up**: the session is closed, which
     * stops the run, and the answer is [Failure.TIMED_OUT]. encri says something at least every thirty
     * seconds while a run works. A caller that is cancelled asks encri to stop the run, which keeps what it
     * copied for the next. Only one run goes at a time: another is [Failure.BUSY].
     */
    @Suppress("ReturnCount", "SwallowedException") // One per way a run ends.
    public suspend fun run(
        folder: BackupFolder,
        keep: Int = DEFAULT_KEEP,
        onProgress: (BackupProgress) -> Unit = {},
    ): EncriResult<BackupRun> {
        if (keep !in 1..MAX_KEEP) return EncriResult.Failed(Failure.INVALID, "keep is 1 to $MAX_KEEP generations")
        if (!running.compareAndSet(false, true)) return EncriResult.Failed(Failure.BUSY, "a run is already going")
        var started = false
        try {
            return withContext(io) {
                if (ended.get()) return@withContext EncriResult.Failed(Failure.ENDED, ENDED_HERE)
                listener.begin()
                val answer =
                    try {
                        remote.run(FolderStub(folder, encriUid), keep)
                    } catch (gone: RemoteException) {
                        return@withContext disconnected()
                    } ?: return@withContext EncriResult.Failed(Failure.FAILED, NO_ANSWER)
                if (answer.status != IEncriService.STATUS_OK) return@withContext failed(answer.status, answer.message)
                started = true
                val end = listener.awaitEnd(SILENCE_MILLIS, onProgress)
                started = false
                when (end) {
                    is RunEnd.Ended -> {
                        runOf(end.result)
                    }

                    RunEnd.Gone -> {
                        disconnected()
                    }

                    RunEnd.Silent -> {
                        close()
                        EncriResult.Failed(Failure.TIMED_OUT, "encri said nothing of the run for two minutes")
                    }
                }
            }
        } finally {
            // Cancelled while the run went on: encri is asked to stop it, keeping what it copied.
            if (started) stop()
            running.set(false)
        }
    }

    /** Asks the run that is going to stop between copies, as a job does when the system stops it. */
    public fun stop() {
        if (!ended.get()) runCatching { remote.stop() }
    }

    /**
     * Stops backups: encri forgets this backup and this app, and nothing in the folder is touched. Backing
     * up again starts at [Encri.setUpBackupIntent], and reconnects to the folder's backup with its passphrase.
     */
    @Suppress("ReturnCount", "SwallowedException") // One per way a request ends.
    public suspend fun stopBackups(): EncriResult<Unit> =
        withContext(io) {
            if (ended.get()) return@withContext EncriResult.Failed(Failure.ENDED, ENDED_HERE)
            val answer =
                try {
                    remote.stopBackups()
                } catch (gone: RemoteException) {
                    return@withContext disconnected()
                } ?: return@withContext EncriResult.Failed(Failure.FAILED, NO_ANSWER)
            if (answer.status !=
                IEncriService.STATUS_OK
            ) {
                failed(answer.status, answer.message)
            } else {
                EncriResult.Ok(Unit)
            }
        }

    /** Ends the session, which stops its run. */
    override fun close() {
        if (!ended.compareAndSet(false, true)) return
        onClosed(this)
        runCatching { remote.end() }
        runCatching { remote.asBinder().unlinkToDeath(death, 0) }
    }

    @Suppress("ReturnCount") // One per way an end is not a run.
    private fun runOf(end: BackupRunResult): EncriResult<BackupRun> {
        if (end.status != IEncriService.STATUS_OK) return failed(end.status, end.message)
        val outcome =
            OUTCOMES[end.outcome]
                ?: return EncriResult.Failed(Failure.FAILED, "encri answered an outcome this client does not know")
        return EncriResult.Ok(
            BackupRun(
                outcome = outcome,
                generation = end.generation.takeIf { it >= 0 },
                documentCount = end.documents.takeIf { it >= 0 },
                contentBytes = end.contentBytes.takeIf { it >= 0 },
                documentsCopied = end.documentsCopied,
                bytesCopied = end.bytesCopied,
                generationsPruned = end.generationsPruned,
                asOfMillis = end.asOfUtcMillis.takeIf { it >= 0 },
                vaultHasDataLoss = end.dataLoss,
            ),
        )
    }

    private fun disconnected(): EncriResult.Failed {
        ended.set(true)
        return EncriResult.Failed(Failure.DISCONNECTED, GONE)
    }

    private fun failed(
        status: Int,
        message: String?,
    ): EncriResult.Failed {
        val failure = failureOf(status)
        if (failure == Failure.ENDED) ended.set(true)
        return EncriResult.Failed(failure, message)
    }

    public companion object {
        /** The generations a run keeps unless the app says otherwise: what encri kept before (encri D95). */
        public const val DEFAULT_KEEP: Int = 8

        /** The most a run may keep (encri-api L27). */
        public const val MAX_KEEP: Int = 1_000

        /** How long a run may go without a word from encri before it is given up. */
        private const val SILENCE_MILLIS = 120_000L

        private const val GONE = "encri's process went away"
        private const val ENDED_HERE = "this session has ended"
        private const val NO_ANSWER = "encri did not answer"

        private val OUTCOMES =
            mapOf(
                IEncriService.RUN_WRITTEN to BackupRun.Outcome.WRITTEN,
                IEncriService.RUN_UNCHANGED to BackupRun.Outcome.UNCHANGED,
                IEncriService.RUN_LANDED to BackupRun.Outcome.LANDED,
                IEncriService.RUN_STOPPED to BackupRun.Outcome.STOPPED,
                IEncriService.RUN_WRONG_FOLDER to BackupRun.Outcome.WRONG_FOLDER,
                IEncriService.RUN_VAULT_CHANGED to BackupRun.Outcome.VAULT_CHANGED,
                IEncriService.RUN_VAULT_INCONSISTENT to BackupRun.Outcome.VAULT_INCONSISTENT,
                IEncriService.RUN_SETTINGS_UNREADABLE to BackupRun.Outcome.SETTINGS_UNREADABLE,
            )

        /** A session over [remote]; synthetic, as [EncriSession.open] is, so no Java caller sees the contract. */
        @JvmSynthetic
        internal fun open(
            remote: IEncriBackup,
            listener: BackupListener,
            io: CoroutineDispatcher,
            encriUid: Int,
            onClosed: (EncriBackup) -> Unit = {},
        ): EncriBackup = EncriBackup(remote, listener, io, encriUid, onClosed)
    }
}
