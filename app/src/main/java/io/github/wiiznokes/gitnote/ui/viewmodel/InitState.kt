package io.github.wiiznokes.gitnote.ui.viewmodel

/**
 * How far setting up a repository has got, and what to put on the button while
 * it is under way. Every step that can take a moment or fail has a state here.
 */
sealed class InitState {
    data object Idle : InitState()
    data class Error(val message: String? = null) : InitState()

    data object GettingAccessToken : InitState()
    data object FetchingRepos : InitState()
    data object GettingUserInfo : InitState()

    data object FetchingInfosSuccess : InitState()

    data object CreatingRemoteRepo : InitState()
    data object AddingDeployKey : InitState()

    data class Cloning(val percent: Int) : InitState()

    data object CalculatingTimestamps : InitState()
    data class GeneratingDatabase(val path: String) : InitState()


    fun message(): String {
        return when (this) {
            AddingDeployKey -> "Adding deploy key"
            CalculatingTimestamps -> "Calculating timestamps"
            is Cloning -> "Cloning: $percent %"
            CreatingRemoteRepo -> "Creating repository"
            is Error -> if (message != null) "Error: $message" else "Error"
            FetchingRepos -> "Fetching repositories"
            is GeneratingDatabase -> "Generating database, path: $path"
            GettingAccessToken -> "Getting the access token"
            GettingUserInfo -> "Getting user information"
            Idle -> ""
            FetchingInfosSuccess -> ""
        }
    }

    fun isLoading(): Boolean = this !is Idle && this !is Error && this !is FetchingInfosSuccess
}
