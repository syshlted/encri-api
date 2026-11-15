package com.systemhalted.encri.client.internal

import com.systemhalted.encri.api.IEncriService
import com.systemhalted.encri.api.IVaultListener
import com.systemhalted.encri.client.VaultEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * What encri tells a session, turned into [VaultEvent]s (L11). encri calls this oneway, on a binder
 * thread, so it never waits: an event a slow collector has not taken is dropped oldest first rather than
 * holding encri's thread.
 */
internal class SessionListener : IVaultListener.Stub() {
    private val emitted =
        MutableSharedFlow<VaultEvent>(extraBufferCapacity = BUFFER, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val events: SharedFlow<VaultEvent> get() = emitted

    /** Whether encri has said the session is over. A request still asks encri, which answers `STATUS_ENDED` (L23). */
    @Volatile
    var toldEnded: Boolean = false
        private set

    /** Ends the session's event stream from this side, when the session's binder died. */
    fun ended() {
        emitted.tryEmit(VaultEvent.Ended)
    }

    override fun onStateChanged(state: Int) {
        when (state) {
            IEncriService.STATE_LOCKED -> emitted.tryEmit(VaultEvent.Locked)

            IEncriService.STATE_UNLOCKED -> emitted.tryEmit(VaultEvent.Unlocked)

            IEncriService.STATE_ENDED -> heardEnded()

            // A state a newer encri added: this client does not know what it means, so says nothing.
            else -> Unit
        }
    }

    private fun heardEnded() {
        toldEnded = true
        emitted.tryEmit(VaultEvent.Ended)
    }

    override fun onFolderChanged(folderId: String?) {
        folderId?.let { emitted.tryEmit(VaultEvent.FolderChanged(it)) }
    }

    private companion object {
        const val BUFFER = 64
    }
}
