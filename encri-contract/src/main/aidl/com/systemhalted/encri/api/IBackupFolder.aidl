// The backup folder, which the backup app serves and encri calls (L27): the five operations of a
// backup store, over the grant the app holds. Only the backup's own files are named here: names that
// begin "encri-" and use [0-9A-Za-z._-], 128 characters at most. Every other name is STATUS_INVALID.
// Its methods are only ever appended.
package com.systemhalted.encri.api;

import com.systemhalted.encri.api.DescriptorResult;
import com.systemhalted.encri.api.FolderPage;
import com.systemhalted.encri.api.Outcome;

interface IBackupFolder {
    /**
     * The folder's backup files in name order, after the name after (null for the first page), at
     * most limit of them. The first page lists the folder, and the rest are served from that listing.
     * A listing the provider says is still loading is never served: it is STATUS_FAILED.
     */
    FolderPage list(@nullable String after, int limit);

    /** The file, for reading: STATUS_NOT_FOUND if it is not there. */
    DescriptorResult openRead(String name);

    /**
     * The file, for writing from its start, created if it is not there and emptied if it is.
     * STATUS_NAME_TAKEN if the provider gave a new file another name.
     */
    DescriptorResult openWrite(String name);

    /** Deletes the file: STATUS_NOT_FOUND if it was not there. */
    Outcome delete(String name);
}
