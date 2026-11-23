package com.systemhalted.encri.client

import android.os.RemoteException
import com.systemhalted.encri.api.IEncriService
import com.systemhalted.encri.client.internal.BackupListener
import com.systemhalted.encri.client.internal.Contract
import com.systemhalted.encri.client.internal.ServiceBinding
import com.systemhalted.encri.client.internal.SessionListener
import com.systemhalted.encri.client.internal.documentOf
import com.systemhalted.encri.client.internal.failureOf
import com.systemhalted.encri.client.internal.rightsOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * A connection to encri's service, from [Encri.connect]. Close it when done: that unbinds, and every
 * session opened through it ends.
 */
public class EncriConnection internal constructor(
    private val binding: ServiceBinding,
    /** The contract version the installed encri implements (L2). */
    public val contractVersion: Int,
    private val io: CoroutineDispatcher,
    /** encri's uid, the only other one a backup folder this connection serves answers. */
    private val encriUid: Int,
) : AutoCloseable {
    private val sessions = ConcurrentHashMap.newKeySet<EncriSession>()
    private val backups = ConcurrentHashMap.newKeySet<EncriBackup>()

    /** Whether encri's service is bound now. After encri's process restarts, Android binds it again. */
    public val isConnected: Boolean get() = binding.service != null

    /**
     * Opens a session for this app (encri D114). A locked vault answers [SessionOutcome.Locked] to every
     * app alike, because a locked vault cannot say which apps it allows.
     */
    @Suppress("ReturnCount", "SwallowedException") // One return per answer.
    public suspend fun openSession(): SessionOutcome =
        withContext(io) {
            val service =
                binding.service
                    ?: return@withContext SessionOutcome.Failed(Failure.DISCONNECTED, "encri's service is not bound")
            val listener = SessionListener()
            val answer =
                try {
                    service.openSession(listener)
                } catch (gone: RemoteException) {
                    return@withContext SessionOutcome.Failed(Failure.DISCONNECTED, gone.message)
                } ?: return@withContext SessionOutcome.Failed(Failure.FAILED, "encri did not answer")
            when (answer.status) {
                IEncriService.STATUS_OK -> {
                    val remote = answer.session
                    val grant = answer.grant
                    if (remote == null || grant == null) {
                        SessionOutcome.Failed(Failure.FAILED, "encri opened a session and sent no grant")
                    } else {
                        val session =
                            EncriSession.open(
                                remote = remote,
                                grant = EncriGrant(rightsOf(grant.rights), documentOf(grant.scope)),
                                listener = listener,
                                io = io,
                                contractVersion = contractVersion,
                                onClosed = sessions::remove,
                            )
                        sessions += session
                        SessionOutcome.Opened(session)
                    }
                }

                IEncriService.STATUS_LOCKED -> {
                    SessionOutcome.Locked
                }

                IEncriService.STATUS_NOT_AUTHORIZED -> {
                    SessionOutcome.NotAuthorized
                }

                IEncriService.STATUS_ACCESS_OFF -> {
                    SessionOutcome.AccessOff
                }

                else -> {
                    SessionOutcome.Failed(failureOf(answer.status), answer.message)
                }
            }
        }

    /**
     * Opens the backup app's session (encri-api L27), for the app the vault's backup was set up with. It opens
     * while the vault is locked: the backup app is recognized without the vault's key.
     */
    @Suppress("ReturnCount", "SwallowedException") // One return per answer.
    public suspend fun openBackup(): BackupOutcome =
        withContext(io) {
            if (contractVersion < Contract.BACKUP_SESSIONS) return@withContext BackupOutcome.TooOld(contractVersion)
            val service =
                binding.service
                    ?: return@withContext BackupOutcome.Failed(Failure.DISCONNECTED, "encri's service is not bound")
            val listener = BackupListener()
            val answer =
                try {
                    service.openBackup(listener)
                } catch (gone: RemoteException) {
                    return@withContext BackupOutcome.Failed(Failure.DISCONNECTED, gone.message)
                } ?: return@withContext BackupOutcome.Failed(Failure.FAILED, "encri did not answer")
            when (answer.status) {
                IEncriService.STATUS_OK -> {
                    val remote =
                        answer.backup
                            ?: return@withContext BackupOutcome.Failed(Failure.FAILED, "encri opened no backup session")
                    val backup = EncriBackup.open(remote, listener, io, encriUid, backups::remove)
                    backups += backup
                    BackupOutcome.Opened(backup)
                }

                IEncriService.STATUS_NOT_AUTHORIZED -> {
                    BackupOutcome.NotSetUp
                }

                IEncriService.STATUS_ACCESS_OFF -> {
                    BackupOutcome.AccessOff
                }

                else -> {
                    BackupOutcome.Failed(failureOf(answer.status), answer.message)
                }
            }
        }

    /** Ends every session opened through this connection, then unbinds. */
    override fun close() {
        sessions.toList().forEach(EncriSession::close)
        backups.toList().forEach(EncriBackup::close)
        binding.unbind()
    }
}
