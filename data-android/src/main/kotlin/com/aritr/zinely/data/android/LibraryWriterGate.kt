package com.aritr.zinely.data.android

/** One exclusive claim over every writer in the local library. */
internal fun interface LibraryWriterLease : AutoCloseable {
    override fun close()
}

/**
 * The library-wide counterpart to [ProjectSessionGate]. It shares the autosave registry rather than
 * adding a second lock, so editor sessions and multi-project repository transactions cannot overlap.
 */
internal fun interface LibraryWriterGate {
    /** Return an exclusive lease, or `null` when an editor/library writer is already active. */
    fun tryAcquire(): LibraryWriterLease?
}

/** Production [LibraryWriterGate] backed by the process-wide autosave ownership registry. */
internal class AutosaveLibraryWriterGate(
    private val factory: AutosaveCoordinatorFactory,
) : LibraryWriterGate {
    override fun tryAcquire(): LibraryWriterLease? = factory.tryAcquireLibraryWrite()
}
