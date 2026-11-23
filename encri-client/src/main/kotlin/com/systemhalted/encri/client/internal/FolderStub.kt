package com.systemhalted.encri.client.internal

import android.os.Binder
import android.os.ParcelFileDescriptor
import android.os.Process
import com.systemhalted.encri.api.DescriptorResult
import com.systemhalted.encri.api.FolderPage
import com.systemhalted.encri.api.IBackupFolder
import com.systemhalted.encri.api.IEncriService
import com.systemhalted.encri.api.Outcome
import com.systemhalted.encri.client.BackupFile
import com.systemhalted.encri.client.BackupFolder
import com.systemhalted.encri.client.BackupNameTakenException
import java.io.FileNotFoundException
import java.io.IOException

/**
 * The app's [BackupFolder] as encri calls it (encri-api L27): the name rule kept, the listing paged, every
 * failure a status, and only encri let in.
 *
 * **Nothing the app throws crosses but its kind.** An exception's message can hold a document's URI or a
 * path, so each failure is sent with a message of the client's own, and the rest stays in the app.
 */
internal class FolderStub(
    private val folder: BackupFolder,
    /** encri's uid. A call from any other process but this one is refused. */
    private val encriUid: Int,
) : IBackupFolder.Stub() {
    /** The listing the first page took, which the rest are served from; one listing at a time. */
    private var listing: List<BackupFile>? = null

    @Synchronized
    @Suppress("ReturnCount") // One per refusal.
    override fun list(
        after: String?,
        limit: Int,
    ): FolderPage {
        if (!fromEncri()) return page(IEncriService.STATUS_NOT_AUTHORIZED, REFUSED)
        if (limit !in 1..MAX_PAGE) return page(IEncriService.STATUS_INVALID, "a page holds 1 to $MAX_PAGE names")
        val files =
            if (after == null) {
                listed() ?: return page(IEncriService.STATUS_FAILED, "the folder could not be listed whole")
            } else {
                listing ?: return page(IEncriService.STATUS_INVALID, "no listing was started")
            }
        val rest = if (after == null) files else files.filter { it.name > after }
        val shown = rest.take(limit)
        return FolderPage().apply {
            status = IEncriService.STATUS_OK
            names = shown.map { it.name }.toTypedArray()
            lengths = shown.map { it.length }.toLongArray()
            modifiedUtcMillis = shown.map { it.modifiedMillis ?: UNKNOWN }.toLongArray()
            next = if (rest.size > limit) shown.last().name else null
        }
    }

    override fun openRead(name: String?): DescriptorResult =
        descriptor(name) { file ->
            folder.openRead(file).let { it to it.statSize }
        }

    override fun openWrite(name: String?): DescriptorResult =
        descriptor(name) { file -> folder.openWrite(file) to UNKNOWN }

    // One return per refusal, and the app's code is not trusted to throw only an IOException.
    @Suppress("ReturnCount", "SwallowedException", "TooGenericExceptionCaught")
    override fun delete(name: String?): Outcome {
        if (!fromEncri()) return outcome(IEncriService.STATUS_NOT_AUTHORIZED, REFUSED)
        if (name == null || !isBackupName(name)) return outcome(IEncriService.STATUS_INVALID, NOT_A_BACKUP_NAME)
        val deleted =
            try {
                folder.delete(name)
            } catch (failed: IOException) {
                return outcome(IEncriService.STATUS_FAILED, NOT_DELETED)
            } catch (failed: RuntimeException) {
                return outcome(IEncriService.STATUS_FAILED, NOT_DELETED)
            }
        return outcome(if (deleted) IEncriService.STATUS_OK else IEncriService.STATUS_NOT_FOUND, null)
    }

    /**
     * The folder's backup files in name order, or null when the app could not list it whole. The kind of
     * failure is the answer, and its message stays here.
     */
    @Suppress("ReturnCount", "SwallowedException", "TooGenericExceptionCaught")
    private fun listed(): List<BackupFile>? {
        listing = null
        val files =
            try {
                folder.list()
            } catch (failed: IOException) {
                return null
            } catch (failed: RuntimeException) {
                return null
            }
        return files
            .filter { isBackupName(it.name) && it.length >= 0 }
            .distinctBy { it.name }
            .sortedBy { it.name }
            .also { listing = it }
    }

    @Suppress("ReturnCount", "SwallowedException", "TooGenericExceptionCaught") // As delete.
    private fun descriptor(
        name: String?,
        open: (String) -> Pair<ParcelFileDescriptor, Long>,
    ): DescriptorResult {
        if (!fromEncri()) return descriptorResult(IEncriService.STATUS_NOT_AUTHORIZED, REFUSED)
        if (name == null ||
            !isBackupName(name)
        ) {
            return descriptorResult(IEncriService.STATUS_INVALID, NOT_A_BACKUP_NAME)
        }
        return try {
            val (opened, size) = open(name)
            DescriptorResult().apply {
                status = IEncriService.STATUS_OK
                descriptor = opened
                length = size
            }
        } catch (taken: BackupNameTakenException) {
            descriptorResult(IEncriService.STATUS_NAME_TAKEN, "the provider gave the file another name")
        } catch (missing: FileNotFoundException) {
            descriptorResult(IEncriService.STATUS_NOT_FOUND, null)
        } catch (failed: IOException) {
            descriptorResult(IEncriService.STATUS_FAILED, "the file would not open")
        } catch (failed: RuntimeException) {
            descriptorResult(IEncriService.STATUS_FAILED, "the file would not open")
        }
    }

    private fun fromEncri(): Boolean = Binder.getCallingUid().let { it == encriUid || it == Process.myUid() }

    private fun page(
        code: Int,
        why: String?,
    ) = FolderPage().apply {
        status = code
        message = why
    }

    private fun outcome(
        code: Int,
        why: String?,
    ) = Outcome().apply {
        status = code
        message = why
    }

    private fun descriptorResult(
        code: Int,
        why: String?,
    ) = DescriptorResult().apply {
        status = code
        message = why
    }

    companion object {
        /** The most names a page holds (encri-api L27), and far under binder's buffer at 128 characters each. */
        const val MAX_PAGE = 1_000

        private const val UNKNOWN = -1L
        private const val REFUSED = "only encri may call the backup folder"
        private const val NOT_A_BACKUP_NAME = "not the name of a backup file"
        private const val NOT_DELETED = "the file could not be deleted"

        private val BACKUP_NAME = Regex("encri-[0-9A-Za-z._-]{1,122}")

        /** Whether [name] is one of the backup's own files, the only names that cross (encri-api L27). */
        fun isBackupName(name: String): Boolean = BACKUP_NAME.matches(name)
    }
}
