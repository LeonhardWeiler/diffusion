package io.github.leonhardweiler.diffusion.manager

/** Where the sync has got to, which is what the cloud button shows. */
sealed interface SyncState {

    /** Nothing has been synced in this session yet. */
    data object Idle : SyncState

    data object Ok : SyncState

    /**
     * @param announce whether the reason should show itself without being asked
     * for. A sync the user started is one they are waiting on, so its failure
     * is an answer; the ones that run on their own when the app opens and
     * closes are not, and a tooltip opening over the list is then just noise —
     * the icon still says it went wrong.
     */
    data class Error(val msg: String?, val announce: Boolean = true) : SyncState

    /**
     * Between the tap and the first thing that reaches the network.
     *
     * That gap is not nothing: the sync waits for the editor's last write,
     * then for the lock, then commits, and only then pulls. The button stood
     * unchanged through all of it, which read as a tap that had not registered.
     * Set by the tap itself rather than by the sync, so that it is there in the
     * same frame the finger leaves the button.
     */
    data object Starting : SyncState

    data object Pull : SyncState

    data object Push : SyncState

    fun isLoading(): Boolean {
        return this is Starting || this is Pull || this is Push
    }

    fun message(): String {
        return when (this) {
            is Error -> this.msg ?: "Unknow Error"
            Idle -> "Sync with the remote"
            Ok -> "Sync done"
            Starting -> "Syncing"
            Pull -> "Pulling"
            Push -> "Pushing"
        }
    }
}

/**
 * How far building the database has got. Only the setup shows this — everywhere
 * else it happens behind a list that is already on screen.
 */
sealed class Progress {
    data class ReadingRepo(val path: String) : Progress()
}
