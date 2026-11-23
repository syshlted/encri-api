package com.systemhalted.encri.api;

parcelable BackupRunResult {
    /** STATUS_OK when the run came to an outcome; otherwise why it did not, such as STATUS_FAILED. */
    int status;
    /** One of IEncriService's RUN_ constants, when status is STATUS_OK. */
    int outcome;
    /** The generation written, landed or found unchanged, or -1. */
    long generation = -1;
    /** How many documents it holds, or -1. */
    int documents = -1;
    /** How many bytes of content they are, or -1. */
    long contentBytes = -1;
    /** Documents whose files this run copied, because the folder lacked them. */
    int documentsCopied;
    long bytesCopied;
    /** Generations pruned, because they fell out of the generations kept. */
    int generationsPruned;
    /** When the vault was as the generation holds it, in UTC milliseconds, or -1. */
    long asOfUtcMillis = -1;
    /** The run found a document damaged or missing; which one is for encri's screens to say. */
    boolean dataLoss;
    @nullable String message;
}
