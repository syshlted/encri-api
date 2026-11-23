package com.systemhalted.encri.client

import android.app.Activity
import android.content.Intent
import android.content.pm.Signature
import android.os.Looper
import com.systemhalted.encri.api.BackupSessionResult
import com.systemhalted.encri.api.IBackupFolder
import com.systemhalted.encri.api.IEncriService
import com.systemhalted.encri.client.internal.sha256Hex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Reaching the backup session and encri's set-up screen (encri-api L27): an encri before version 3 is
 * never asked, each refusal is its own outcome, and the folder is never handed to an encri this app does
 * not trust.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 36])
class BackupConnectionTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun `an encri before version 3 is too old, and is not asked`() {
        installEncri(context, STRANGER)
        val service = FakeService(version = 2)
        serveEncri(service)
        val connection = connected()
        assertEquals(BackupOutcome.TooOld(2), onWorker { connection.openBackup() })
        assertNull("an older encri was asked for a method it does not have", service.backupListener)
        connection.close()
    }

    @Test
    fun `each answer to opening the backup session is its own outcome`() {
        installEncri(context, STRANGER)
        val service = FakeService()
        serveEncri(service)
        val connection = connected()
        service.openingBackup = { answer(IEncriService.STATUS_NOT_AUTHORIZED) }
        assertEquals(BackupOutcome.NotSetUp, onWorker { connection.openBackup() })
        service.openingBackup = { answer(IEncriService.STATUS_ACCESS_OFF) }
        assertEquals(BackupOutcome.AccessOff, onWorker { connection.openBackup() })
        service.openingBackup = { null }
        assertEquals(Failure.FAILED, (onWorker { connection.openBackup() } as BackupOutcome.Failed).failure)
        service.openingBackup = { answer(IEncriService.STATUS_OK) }
        assertEquals(Failure.FAILED, (onWorker { connection.openBackup() } as BackupOutcome.Failed).failure)
        val remote = FakeBackup()
        service.openingBackup = { answer(IEncriService.STATUS_OK).apply { backup = remote } }
        val backup = (onWorker { connection.openBackup() } as BackupOutcome.Opened).backup
        assertTrue(backup.isOpen)
        assertNotNull("encri was given no listener", service.backupListener)
        connection.close()
        assertFalse("the backup session outlived its connection", backup.isOpen)
        assertTrue("encri was not told the backup session ended", remote.ended)
    }

    @Test
    fun `the set-up screen is handed the folder and its provider`() {
        installEncri(context, STRANGER)
        val intent =
            Encri.setUpBackupIntent(
                context,
                listedFolder("encri-backup.keys"),
                "com.example.cloud",
                trustStranger(),
            )!!
        assertEquals(IEncriService.ACTION_SET_UP_BACKUP, intent.action)
        assertEquals(IEncriService.PACKAGE, intent.`package`)
        assertEquals("com.example.cloud", intent.getStringExtra(IEncriService.EXTRA_BACKUP_FOLDER_AUTHORITY))
        val folder = IBackupFolder.Stub.asInterface(intent.extras!!.getBinder(IEncriService.EXTRA_BACKUP_FOLDER))
        assertEquals(listOf("encri-backup.keys"), folder.list(null, 10).names!!.toList())
    }

    @Test
    fun `the folder is never handed to an encri this app does not trust, or to none`() {
        assertNull(Encri.setUpBackupIntent(context, EmptyFolder, "com.example.cloud"))
        installEncri(context, STRANGER)
        assertNull(Encri.setUpBackupIntent(context, EmptyFolder, "com.example.cloud"))
    }

    @Test
    fun `the set-up screen's answer is what it set up, or nothing`() {
        fun result(code: Int) = Intent().putExtra(IEncriService.EXTRA_BACKUP_SET_UP, code)
        assertEquals(
            BackupSetUp.CREATED,
            Encri.backupSetUpResult(Activity.RESULT_OK, result(IEncriService.BACKUP_CREATED)),
        )
        assertEquals(
            BackupSetUp.RECONNECTED,
            Encri.backupSetUpResult(Activity.RESULT_OK, result(IEncriService.BACKUP_RECONNECTED)),
        )
        assertNull(Encri.backupSetUpResult(Activity.RESULT_CANCELED, result(IEncriService.BACKUP_CREATED)))
        assertNull(Encri.backupSetUpResult(Activity.RESULT_OK, null))
        assertNull(Encri.backupSetUpResult(Activity.RESULT_OK, result(0)))
    }

    @Test
    fun `the unlock the backup app hears is the contract's`() {
        assertEquals(IEncriService.ACTION_VAULT_UNLOCKED, Encri.ACTION_VAULT_UNLOCKED)
    }

    private fun answer(code: Int) = BackupSessionResult().apply { status = code }

    private fun trustStranger() = EncriTrust.alsoTrusting(sha256Hex(STRANGER.toByteArray()))

    private fun connected(): EncriConnection =
        (onWorker { Encri.connect(context, trustStranger(), Dispatchers.IO) } as ConnectResult.Connected).connection

    /** As `EncriConnectionTest`'s: a bind's answer arrives on the main looper, which only this thread runs. */
    private fun <T> onWorker(block: suspend () -> T): T {
        val running = CoroutineScope(Dispatchers.IO).async { block() }
        while (!running.isCompleted) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(POLL_MILLIS)
        }
        return runBlocking { running.await() }
    }

    private companion object {
        val STRANGER = Signature(ByteArray(64) { 7 })
        const val POLL_MILLIS = 5L
    }
}
