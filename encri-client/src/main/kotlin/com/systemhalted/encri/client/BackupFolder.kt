package com.systemhalted.encri.client

import android.os.ParcelFileDescriptor
import java.io.IOException

/**
 * The backup folder, as the backup app serves it to encri (encri-api L27): the four things a backup run
 * asks of a folder. The app implements it over the grant it holds, and encri backs up into it, calling
 * it from the app's binder threads during [EncriBackup.run] and encri's set-up screen, never at once and
 * each with a minute to answer.
 *
 * **Only the backup's own files reach it**: names that begin `encri-` and use `[0-9A-Za-z._-]`, at most
 * 128 characters. The client refuses every other name before asking, and leaves every other file out of
 * what [list] answers, so the rest of the user's folder never reaches encri. Nothing the app throws
 * reaches encri either: each failure crosses as a status, without its message, so no path can.
 */
public interface BackupFolder {
    /**
     * Every file in the folder, at its length now, and never a subfolder. **The whole folder or an
     * [IOException]**: a provider that says its listing is still loading is asked again and then refused,
     * because encri deletes on what a listing names.
     */
    @Throws(IOException::class)
    public fun list(): List<BackupFile>

    /** The file, opened for reading. A [java.io.FileNotFoundException] when it is not there. */
    @Throws(IOException::class)
    public fun openRead(name: String): ParcelFileDescriptor

    /**
     * The file, opened for writing from its start: created when it is not there, emptied when it is. A
     * [BackupNameTakenException] when the provider gave the new file another name.
     */
    @Throws(IOException::class)
    public fun openWrite(name: String): ParcelFileDescriptor

    /** Deletes the file. True when it was deleted, false when it was not there. */
    @Throws(IOException::class)
    public fun delete(name: String): Boolean
}

/** A file in the backup folder, as [BackupFolder.list] answers it. */
public data class BackupFile(
    val name: String,
    val length: Long,
    /** When it was last written, or null when the provider does not say. */
    val modifiedMillis: Long?,
)

/** The provider gave a file created as one name another (encri-api L27): the name is refused, never renamed around. */
public class BackupNameTakenException(
    message: String,
) : IOException(message)
