package com.systemhalted.encri.client.internal

import com.systemhalted.encri.api.BackupRunResult
import com.systemhalted.encri.api.IBackupListener
import com.systemhalted.encri.api.IEncriService
import com.systemhalted.encri.client.BackupProgress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What encri tells the backup session about its runs (encri-api L27). encri calls it oneway, on a binder
 * thread, so nothing here waits: the newest progress replaces one not yet read, and a run's end is kept
 * until it is read.
 */
internal class BackupListener : IBackupListener.Stub() {
    /** The run's end, or null when encri's process went away under it. */
    @Volatile
    private var ending = CompletableDeferred<BackupRunResult?>()

    /** Progress, or [HEARD] for a word this client does not know, which still says encri is working. */
    private val progress = Channel<Any>(Channel.CONFLATED)

    /** Starts listening for a new run's end, before the run is asked for, since its end may come first. */
    fun begin() {
        ending = CompletableDeferred()
        while (progress.tryReceive().isSuccess) Unit
    }

    /** encri's process went away: a run being waited on has ended without a word. */
    fun gone() {
        ending.complete(null)
    }

    /**
     * How the run ended, telling [onProgress] what it hears on the way; [RunEnd.Silent] when encri says
     * nothing for [silenceMillis], progress or end.
     */
    @Suppress("ReturnCount") // One per way the wait ends.
    suspend fun awaitEnd(
        silenceMillis: Long,
        onProgress: (BackupProgress) -> Unit,
    ): RunEnd {
        val end = ending
        while (true) {
            val heard =
                withTimeoutOrNull(silenceMillis) {
                    select<Any> {
                        end.onAwait { it ?: RunEnd.Gone }
                        progress.onReceive { it }
                    }
                } ?: return RunEnd.Silent
            when (heard) {
                is BackupRunResult -> return RunEnd.Ended(heard)
                RunEnd.Gone -> return RunEnd.Gone
                is BackupProgress -> onProgress(heard)
                else -> Unit
            }
        }
    }

    override fun onRunProgress(
        phase: Int,
        done: Long,
        total: Long,
    ) {
        val named =
            when (phase) {
                IEncriService.PHASE_PREPARING -> BackupProgress.Phase.PREPARING

                IEncriService.PHASE_COPYING -> BackupProgress.Phase.COPYING

                // A phase a newer encri added: not reported, but it still says encri is working.
                else -> null
            }
        progress.trySend(named?.let { BackupProgress(it, done, total.takeIf { all -> all >= 0 }) } ?: HEARD)
    }

    override fun onRunFinished(result: BackupRunResult?) {
        ending.complete(result ?: BackupRunResult().apply { status = IEncriService.STATUS_FAILED })
    }

    private object HEARD
}

/** How a run being waited on ended. */
internal sealed interface RunEnd {
    data class Ended(
        val result: BackupRunResult,
    ) : RunEnd

    /** encri's process went away. */
    data object Gone : RunEnd

    /** encri said nothing for too long. */
    data object Silent : RunEnd
}
