package io.github.leonhardweiler.diffusion.manager

sealed interface SyncState {
    data object Idle : SyncState

    data object Ok : SyncState

    data class Error(val msg: String?, val announce: Boolean = true) : SyncState

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

sealed class Progress {
    data class ReadingRepo(val path: String) : Progress()
}
