package io.github.leonhardweiler.diffusion.ui.viewmodel

/**
 * How far setting up a repository has got, and what to put on the button while
 * it is under way. Every step that can take a moment or fail has a state here.
 */
sealed class InitState {
    data object Idle : InitState()
    data class Error(val message: String? = null) : InitState()

    data class Cloning(val percent: Int) : InitState()

    /**
     * Reading a repository that is already on the device. Between the tap on
     * the folder and the next screen there is a second or two of libgit2 and of
     * walking the whole working tree, and without this the app looked like it
     * had ignored the tap.
     */
    data object OpeningRepo : InitState()

    data class GeneratingDatabase(val path: String) : InitState()


    fun message(): String {
        return when (this) {
            is Cloning -> "Cloning: $percent %"
            is Error -> if (message != null) "Error: $message" else "Error"
            is GeneratingDatabase -> "Generating database, path: $path"
            OpeningRepo -> "Opening the repository…"
            Idle -> ""
        }
    }

    fun isLoading(): Boolean = this !is Idle && this !is Error
}
