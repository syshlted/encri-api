package com.systemhalted.encri.api;

parcelable FolderPage {
    int status;
    /** The files' names, in name order. */
    @nullable String[] names;
    /** Each file's length in bytes, by position. */
    @nullable long[] lengths;
    /** When each was last written, in UTC milliseconds, by position; -1 when the provider does not say. */
    @nullable long[] modifiedUtcMillis;
    /** What to pass as after for the next page, or null when this is the last. */
    @nullable String next;
    @nullable String message;
}
