package com.systemhalted.encri.client

import android.os.Process
import com.systemhalted.encri.api.BackupRunResult
import com.systemhalted.encri.api.BackupStateResult
import com.systemhalted.encri.api.IBackupFolder
import com.systemhalted.encri.api.IEncriBackup
import com.systemhalted.encri.api.IEncriService
import com.systemhalted.encri.api.Outcome
import com.systemhalted.encri.client.internal.BackupListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * What the backup session makes of encri's answers (encri-api L27): a run answers at once and ends on the
 * listener, a silent encri is given up, and a cancelled caller stops the run. Against a fake, in this
 * process, on virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27, 36])
class EncriBackupTest {
    private val fake = FakeBackup()
    private val listener = BackupListener()

    private fun TestScope.backup() =
        EncriBackup.open(fake, listener, UnconfinedTestDispatcher(testScheduler), Process.myUid())

    @Test
    fun `a run that ends is the outcome encri sent, with what it heard on the way`() =
        runTest {
            val heard = mutableListOf<BackupProgress>()
            val run = async { backup().run(EmptyFolder, keep = 12) { heard += it } }
            runCurrent()
            assertEquals(12, fake.keep)
            listener.onRunProgress(IEncriService.PHASE_PREPARING, 3, 10)
            runCurrent()
            listener.onRunProgress(IEncriService.PHASE_COPYING, 4_096, -1)
            runCurrent()
            listener.onRunFinished(written())
            val done = (run.await() as EncriResult.Ok).value
            assertEquals(BackupRun.Outcome.WRITTEN, done.outcome)
            assertEquals(7L, done.generation)
            assertEquals(4_096L, done.bytesCopied)
            assertEquals(3_000L, done.asOfMillis)
            assertTrue(done.vaultHasDataLoss)
            assertEquals(
                listOf(
                    BackupProgress(BackupProgress.Phase.PREPARING, 3, 10),
                    BackupProgress(BackupProgress.Phase.COPYING, 4_096, null),
                ),
                heard,
            )
        }

    @Test
    fun `what encri does not know reads as null, not as a number`() =
        runTest {
            val run = async { backup().run(EmptyFolder) }
            runCurrent()
            listener.onRunFinished(
                BackupRunResult().apply {
                    status = IEncriService.STATUS_OK
                    outcome = IEncriService.RUN_STOPPED
                },
            )
            val stopped = (run.await() as EncriResult.Ok).value
            assertEquals(BackupRun.Outcome.STOPPED, stopped.outcome)
            assertNull(stopped.generation)
            assertNull(stopped.documentCount)
            assertNull(stopped.contentBytes)
            assertNull(stopped.asOfMillis)
        }

    @Test
    fun `an end that came before the run's answer is not missed`() =
        runTest {
            // The listener is oneway, so encri's word can arrive before its answer to run.
            fake.running = { _, _ ->
                listener.onRunFinished(written())
                ok()
            }
            assertEquals(BackupRun.Outcome.WRITTEN, (backup().run(EmptyFolder) as EncriResult.Ok).value.outcome)
        }

    @Test
    fun `a run encri refuses is the failure it names, and nothing is waited on`() =
        runTest {
            fake.running = { _, _ -> declined(IEncriService.STATUS_LOCKED) }
            assertEquals(Failure.LOCKED, (backup().run(EmptyFolder) as EncriResult.Failed).failure)
            fake.running = { _, _ -> declined(IEncriService.STATUS_BUSY) }
            assertEquals(Failure.BUSY, (backup().run(EmptyFolder) as EncriResult.Failed).failure)
            fake.running = { _, _ -> null }
            assertEquals(Failure.FAILED, (backup().run(EmptyFolder) as EncriResult.Failed).failure)
        }

    @Test
    fun `a run that ends in failure is the failure encri sent`() =
        runTest {
            val run = async { backup().run(EmptyFolder) }
            runCurrent()
            listener.onRunFinished(
                BackupRunResult().apply {
                    status = IEncriService.STATUS_FAILED
                    message = "the backup app did not answer"
                },
            )
            assertEquals(EncriResult.Failed(Failure.FAILED, "the backup app did not answer"), run.await())
        }

    @Test
    fun `an outcome this client does not know is a failure, not a guess`() =
        runTest {
            val run = async { backup().run(EmptyFolder) }
            runCurrent()
            listener.onRunFinished(
                BackupRunResult().apply {
                    status = IEncriService.STATUS_OK
                    outcome = 99
                },
            )
            assertEquals(Failure.FAILED, (run.await() as EncriResult.Failed).failure)
        }

    @Test
    fun `a run encri says nothing of for two minutes is given up, and the session with it`() =
        runTest {
            val session = backup()
            val run = async { session.run(EmptyFolder) }
            runCurrent()
            advanceTimeBy(119_000)
            assertFalse("given up before two minutes", run.isCompleted)
            advanceTimeBy(2_000)
            assertEquals(Failure.TIMED_OUT, (run.await() as EncriResult.Failed).failure)
            assertFalse(session.isOpen)
            assertTrue("encri was not told the session ended", fake.ended)
        }

    @Test
    fun `progress keeps a long run from being given up`() =
        runTest {
            val run = async { backup().run(EmptyFolder) }
            runCurrent()
            repeat(4) {
                advanceTimeBy(100_000)
                listener.onRunProgress(IEncriService.PHASE_COPYING, it.toLong(), 10)
                runCurrent()
            }
            listener.onRunFinished(written())
            assertTrue(run.await() is EncriResult.Ok)
        }

    @Test
    fun `a phase a newer encri added still counts as encri talking`() =
        runTest {
            val heard = mutableListOf<BackupProgress>()
            val run = async { backup().run(EmptyFolder) { heard += it } }
            runCurrent()
            advanceTimeBy(100_000)
            listener.onRunProgress(77, 1, 1)
            runCurrent()
            advanceTimeBy(100_000)
            listener.onRunFinished(written())
            assertTrue(run.await() is EncriResult.Ok)
            assertEquals(emptyList<BackupProgress>(), heard)
        }

    @Test
    fun `encri's process going away ends the wait at once`() =
        runTest {
            val run = async { backup().run(EmptyFolder) }
            runCurrent()
            listener.gone()
            assertEquals(Failure.DISCONNECTED, (run.await() as EncriResult.Failed).failure)
        }

    @Test
    fun `a caller that is cancelled asks encri to stop the run`() =
        runTest {
            val running = launch { backup().run(EmptyFolder) }
            runCurrent()
            running.cancel()
            runCurrent()
            assertEquals(1, fake.stops)
        }

    @Test
    fun `a run that ends asks nothing to stop`() =
        runTest {
            val run = async { backup().run(EmptyFolder) }
            runCurrent()
            listener.onRunFinished(written())
            run.await()
            assertEquals(0, fake.stops)
        }

    @Test
    fun `a generation count out of range is refused without asking encri`() =
        runTest {
            assertEquals(Failure.INVALID, (backup().run(EmptyFolder, keep = 0) as EncriResult.Failed).failure)
            assertEquals(
                Failure.INVALID,
                (backup().run(EmptyFolder, keep = EncriBackup.MAX_KEEP + 1) as EncriResult.Failed).failure,
            )
            assertEquals(0, fake.runs)
        }

    @Test
    fun `a second run while one goes is busy, and encri is not asked`() =
        runTest {
            val session = backup()
            val first = async { session.run(EmptyFolder) }
            runCurrent()
            assertEquals(Failure.BUSY, (session.run(EmptyFolder) as EncriResult.Failed).failure)
            assertEquals(1, fake.runs)
            listener.onRunFinished(written())
            first.await()
        }

    @Test
    fun `the folder encri is handed answers for the app's folder`() =
        runTest {
            val run = async { backup().run(listedFolder("encri-backup.keys")) }
            runCurrent()
            val page = fake.folder!!.list(null, 10)
            assertEquals(listOf("encri-backup.keys"), page.names!!.toList())
            listener.onRunFinished(written())
            run.await()
        }

    @Test
    fun `the state is what encri says, and no answer is a failure`() =
        runTest {
            fake.stating = {
                BackupStateResult().apply {
                    status = IEncriService.STATUS_OK
                    setUp = true
                    locked = true
                    waiting = true
                }
            }
            assertEquals(
                EncriResult.Ok(BackupState(isSetUp = true, isLocked = true, hasRunWaiting = true)),
                backup().state(),
            )
            fake.stating = { null }
            assertEquals(Failure.FAILED, (backup().state() as EncriResult.Failed).failure)
        }

    @Test
    fun `stopping backups is what encri answers`() =
        runTest {
            fake.stoppingBackups = { ok() }
            assertEquals(EncriResult.Ok(Unit), backup().stopBackups())
            fake.stoppingBackups = { declined(IEncriService.STATUS_ENDED) }
            val session = backup()
            assertEquals(Failure.ENDED, (session.stopBackups() as EncriResult.Failed).failure)
            assertFalse("a session encri said had ended is still open", session.isOpen)
        }

    @Test
    fun `a closed session asks encri nothing`() =
        runTest {
            val session = backup()
            session.close()
            assertTrue(fake.ended)
            assertEquals(Failure.ENDED, (session.run(EmptyFolder) as EncriResult.Failed).failure)
            assertEquals(Failure.ENDED, (session.state() as EncriResult.Failed).failure)
            assertEquals(0, fake.runs)
        }

    private fun written() =
        BackupRunResult().apply {
            status = IEncriService.STATUS_OK
            outcome = IEncriService.RUN_WRITTEN
            generation = 7
            documents = 12
            contentBytes = 8_192
            documentsCopied = 2
            bytesCopied = 4_096
            asOfUtcMillis = 3_000
            dataLoss = true
        }
}

/** A backup session whose every answer a test sets. Unset answers are null: an older encri (L2). */
internal class FakeBackup : IEncriBackup.Stub() {
    var stating: () -> BackupStateResult? = { null }
    var running: (IBackupFolder?, Int) -> Outcome? = { _, _ -> ok() }
    var stoppingBackups: () -> Outcome? = { null }
    var folder: IBackupFolder? = null
    var keep = 0
    var runs = 0
    var stops = 0
    var ended = false

    override fun state(): BackupStateResult? = stating()

    override fun run(
        folder: IBackupFolder?,
        keep: Int,
    ): Outcome? {
        runs++
        this.folder = folder
        this.keep = keep
        return running(folder, keep)
    }

    override fun stop() {
        stops++
    }

    override fun stopBackups(): Outcome? = stoppingBackups()

    override fun end() {
        ended = true
    }
}

internal fun ok() = Outcome().apply { status = IEncriService.STATUS_OK }

internal fun declined(status: Int): Outcome = Outcome().apply { this.status = status }

/** A folder with nothing in it. */
internal object EmptyFolder : BackupFolder by listedFolder()

internal fun listedFolder(vararg names: String): BackupFolder = MemoryFolder(names.map { BackupFile(it, 0, null) })
