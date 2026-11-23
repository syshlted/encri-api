package com.systemhalted.encri.client

import android.os.ParcelFileDescriptor
import android.os.Process
import com.systemhalted.encri.api.IEncriService
import com.systemhalted.encri.client.internal.FolderStub
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBinder
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * The backup folder as encri calls it (encri-api L27): only the backup's own names cross, a listing is
 * whole or refused and paged in name order, nothing the app throws crosses but its kind, and no process
 * but encri's is answered.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 36])
class BackupFolderTest {
    private val folder =
        MemoryFolder(
            listOf(
                BackupFile("encri-gen-0000000002-00000000000000000000000000000000.encri", 300, 2_000),
                BackupFile("photo.jpg", 10, 1_000),
                BackupFile("encri-backup.keys", 8_192, null),
                BackupFile("encri-blob-b.encri", 200, 3_000),
                BackupFile("encri-blob-a.encri", 100, 4_000),
                BackupFile("encri-blob-a.encri", 100, 4_000),
                BackupFile("encri-../escape", 1, 1),
                BackupFile("encri-" + "x".repeat(123), 1, 1),
                BackupFile("encri-negative", -1, 1),
            ),
        )
    private val stub = FolderStub(folder, ENCRI_UID)

    @After
    fun callerIsThisProcessAgain() = ShadowBinder.reset()

    @Test
    fun `only the backup's own files are listed, once each, in name order`() {
        val page = stub.list(null, FolderStub.MAX_PAGE)
        assertEquals(IEncriService.STATUS_OK, page.status)
        assertEquals(
            listOf(
                "encri-backup.keys",
                "encri-blob-a.encri",
                "encri-blob-b.encri",
                "encri-gen-0000000002-00000000000000000000000000000000.encri",
            ),
            page.names!!.toList(),
        )
        assertEquals(listOf(8_192L, 100L, 200L, 300L), page.lengths!!.toList())
        assertEquals(listOf(-1L, 4_000L, 3_000L, 2_000L), page.modifiedUtcMillis!!.toList())
        assertNull(page.next)
    }

    @Test
    fun `a listing is paged from the listing the first page took`() {
        val first = stub.list(null, 3)
        assertEquals(3, first.names!!.size)
        assertEquals("encri-blob-b.encri", first.next)
        folder.files.clear()
        val second = stub.list(first.next, 3)
        assertEquals(
            listOf("encri-gen-0000000002-00000000000000000000000000000000.encri"),
            second.names!!.toList(),
        )
        assertNull(second.next)
    }

    @Test
    fun `a page with no first page, or of the wrong size, is refused`() {
        assertEquals(IEncriService.STATUS_INVALID, stub.list("encri-backup.keys", 10).status)
        assertEquals(IEncriService.STATUS_INVALID, stub.list(null, 0).status)
        assertEquals(IEncriService.STATUS_INVALID, stub.list(null, FolderStub.MAX_PAGE + 1).status)
    }

    @Test
    fun `a folder that cannot be listed whole is refused, and says nothing of why`() {
        folder.failing = IOException("content://com.example.cloud/tree/secret-folder is still loading")
        val page = stub.list(null, 10)
        assertEquals(IEncriService.STATUS_FAILED, page.status)
        assertFalse(page.message.orEmpty().contains("secret"))
        assertNull(page.names)
    }

    @Test
    fun `a file opened for reading carries its length`() {
        val opened = stub.openRead("encri-blob-a.encri")
        assertEquals(IEncriService.STATUS_OK, opened.status)
        assertEquals(5L, opened.length)
        opened.descriptor!!.close()
    }

    @Test
    fun `a file that is not there is not found, and says nothing of where`() {
        val missing = stub.openRead("encri-blob-z.encri")
        assertEquals(IEncriService.STATUS_NOT_FOUND, missing.status)
        assertNull(missing.message)
    }

    @Test
    fun `a file that will not open is a failure without its message`() {
        folder.failing = IOException("/storage/emulated/0/Secret/encri-blob-a.encri")
        val broken = stub.openRead("encri-blob-a.encri")
        assertEquals(IEncriService.STATUS_FAILED, broken.status)
        assertFalse(broken.message.orEmpty().contains("Secret"))
    }

    @Test
    fun `a name the provider changed is taken, not renamed around`() {
        assertEquals(IEncriService.STATUS_NAME_TAKEN, stub.openWrite(MemoryFolder.TAKEN).status)
        val written = stub.openWrite("encri-blob-c.encri")
        assertEquals(IEncriService.STATUS_OK, written.status)
        assertEquals(-1L, written.length)
        written.descriptor!!.close()
    }

    @Test
    fun `a name that is not the backup's is refused before the app is asked`() {
        listOf("photo.jpg", "encri-../escape", "encri-", "encri-" + "x".repeat(123), "../encri-blob-a.encri").forEach {
            assertEquals(it, IEncriService.STATUS_INVALID, stub.openRead(it).status)
            assertEquals(it, IEncriService.STATUS_INVALID, stub.openWrite(it).status)
            assertEquals(it, IEncriService.STATUS_INVALID, stub.delete(it).status)
        }
        assertEquals(IEncriService.STATUS_INVALID, stub.openRead(null).status)
        assertTrue("the app was asked about a name that is not the backup's", folder.asked.isEmpty())
    }

    @Test
    fun `a delete says whether the file was there`() {
        assertEquals(IEncriService.STATUS_OK, stub.delete("encri-blob-a.encri").status)
        assertEquals(IEncriService.STATUS_NOT_FOUND, stub.delete("encri-blob-a.encri").status)
        folder.failing = IOException("no")
        assertEquals(IEncriService.STATUS_FAILED, stub.delete("encri-blob-b.encri").status)
    }

    @Test
    fun `an app that throws something else is a failure, not a crash of the binder thread`() {
        folder.failing = SecurityException("the grant was revoked")
        assertEquals(IEncriService.STATUS_FAILED, stub.list(null, 10).status)
        assertEquals(IEncriService.STATUS_FAILED, stub.openRead("encri-blob-a.encri").status)
        assertEquals(IEncriService.STATUS_FAILED, stub.delete("encri-blob-a.encri").status)
    }

    @Test
    fun `encri's process is answered, and no other`() {
        ShadowBinder.setCallingUid(ENCRI_UID)
        assertEquals(IEncriService.STATUS_OK, stub.list(null, 10).status)
        ShadowBinder.setCallingUid(ENCRI_UID + 1)
        assertEquals(IEncriService.STATUS_NOT_AUTHORIZED, stub.list(null, 10).status)
        assertEquals(IEncriService.STATUS_NOT_AUTHORIZED, stub.openRead("encri-blob-a.encri").status)
        assertEquals(IEncriService.STATUS_NOT_AUTHORIZED, stub.openWrite("encri-blob-a.encri").status)
        assertEquals(IEncriService.STATUS_NOT_AUTHORIZED, stub.delete("encri-blob-a.encri").status)
        assertTrue("the app was asked on another process's behalf", folder.asked.isEmpty())
    }

    private companion object {
        val ENCRI_UID = Process.myUid() + 7
    }
}

/** A backup folder in memory whose descriptors are small temporary files, and whose failures a test sets. */
internal class MemoryFolder(
    listed: List<BackupFile>,
) : BackupFolder {
    val files = listed.toMutableList()
    var failing: Exception? = null
    val asked = mutableListOf<String>()

    override fun list(): List<BackupFile> {
        failing?.let { throw it }
        return files.toList()
    }

    override fun openRead(name: String): ParcelFileDescriptor {
        asked += name
        failing?.let { throw it }
        if (files.none { it.name == name }) throw FileNotFoundException("content://com.example.cloud/document/$name")
        return descriptor(ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun openWrite(name: String): ParcelFileDescriptor {
        asked += name
        failing?.let { throw it }
        if (name == TAKEN) throw BackupNameTakenException("the provider called it $name (1)")
        return descriptor(ParcelFileDescriptor.MODE_WRITE_ONLY)
    }

    override fun delete(name: String): Boolean {
        asked += name
        failing?.let { throw it }
        return files.removeAll { it.name == name }
    }

    private fun descriptor(mode: Int): ParcelFileDescriptor {
        val file =
            File.createTempFile("folder", ".encri").apply {
                writeText("blob!")
                deleteOnExit()
            }
        return ParcelFileDescriptor.open(file, mode)
    }

    companion object {
        const val TAKEN = "encri-blob-taken.encri"
    }
}
