package io.github.leonhardweiler.diffusion.ui.viewmodel

sealed class InitState {
    data object Idle : InitState()
    data class Error(val message: String? = null) : InitState()

    data class Cloning(val percent: Int) : InitState()

    data object OpeningRepo : InitState()

    data class ReadingRepo(val path: String) : InitState()

    data object SyncingRepo : InitState()

    fun message(): String {
        return when (this) {
            is Cloning -> "Cloning: $percent %"
            is Error -> if (message != null) "Error: $message" else "Error"
            is ReadingRepo -> "Reading the repository, path: $path"
            OpeningRepo -> "Opening the repository…"
            SyncingRepo -> "Syncing with the remote…"
            Idle -> ""
        }
    }

    fun isLoading(): Boolean = this !is Idle && this !is Error
}
