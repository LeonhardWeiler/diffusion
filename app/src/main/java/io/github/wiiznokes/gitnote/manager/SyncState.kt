package io.github.wiiznokes.gitnote.manager

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

    data object Pull : SyncState

    data object Push : SyncState

    fun isLoading(): Boolean {
        return this is Pull || this is Push
    }

    fun message(): String {
        return when (this) {
            is Error -> this.msg ?: "Unknow Error"
            Idle -> "Sync with the remote"
            Ok -> "Sync done"
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
    data class GeneratingDatabase(val path: String) : Progress()
}
