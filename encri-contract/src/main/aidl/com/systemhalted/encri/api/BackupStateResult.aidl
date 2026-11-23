package com.systemhalted.encri.api;

parcelable BackupStateResult {
    int status;
    /** A backup is set up, with this app. */
    boolean setUp;
    boolean locked;
    /** A run stopped part way and waits to land, which it can locked or not. */
    boolean waiting;
    @nullable String message;
}
