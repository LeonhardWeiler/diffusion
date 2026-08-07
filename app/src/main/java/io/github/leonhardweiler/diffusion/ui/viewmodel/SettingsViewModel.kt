package io.github.leonhardweiler.diffusion.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.manager.RepoSession
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val repoManager = MyApp.appModule.repoManager
    val uiHelper = MyApp.appModule.uiHelper

    // Storage work outlives the screen that started it, see AppModule.appScope.
    private val appScope = MyApp.appModule.appScope

    /** The repository whose notes are being looked at. */
    val activeRepo: RepoSession get() = repoManager.active.value

    fun update(f: suspend () -> Unit) {
        viewModelScope.launch {
            f()
        }
    }

    /**
     * Points a repository somewhere else.
     *
     * Both halves are needed and only one of them was here: push and pull ask
     * the repository for its remote, the preference is what the app shows and
     * what tells it whether there is a remote at all. Writing only the
     * preference left a settings screen saying one address while every sync
     * went to another.
     */
    fun updateRemoteUrl(repo: RepoSession, url: String) {
        val trimmed = url.trim()

        appScope.launch {
            repo.prefs.remoteUrl.update(trimmed)

            // An emptied field means "do not sync", which the preference above
            // already says. Writing it through would ask git for a remote
            // without an address, and the repository is better left with the
            // one it has than with a broken one.
            if (trimmed.isEmpty()) return@launch

            repo.gitManager.setRemoteUrl(trimmed).onFailure {
                uiHelper.makeToast(it.message)
            }
        }
    }

    /**
     * Lets go of a repository. The folder and its notes are not touched — this
     * is the app forgetting where they are.
     *
     * @param onChanged called when the repository that was let go of was the one
     * being looked at, so the screen it was showing can be left.
     */
    fun removeRepo(repo: RepoSession, onChanged: () -> Unit) {
        val wasActive = repo.id == activeRepo.id

        appScope.launch {
            repoManager.remove(repo.id)

            if (wasActive) {
                viewModelScope.launch { onChanged() }
            }
        }
    }

    fun reloadIndex() {
        appScope.launch {
            val res = activeRepo.storageManager.rebuildIndex()
            res.onFailure {
                uiHelper.makeToast("$it")
            }
            res.onSuccess {
                uiHelper.makeToast(uiHelper.getString(R.string.success_reload))
            }
        }
    }
}
