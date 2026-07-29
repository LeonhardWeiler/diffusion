package io.github.leonhardweiler.diffusion.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.AppPreferences
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val storageManager = MyApp.appModule.storageManager
    private val gitManager = MyApp.appModule.gitManager
    val uiHelper = MyApp.appModule.uiHelper

    // Storage work outlives the screen that started it, see AppModule.appScope.
    private val appScope = MyApp.appModule.appScope

    fun update(f: suspend () -> Unit) {
        viewModelScope.launch {
            f()
        }
    }

    /**
     * Points the repository somewhere else.
     *
     * Both halves are needed and only one of them was here: push and pull ask
     * the repository for its remote, the preference is what the app shows and
     * what tells it whether there is a remote at all. Writing only the
     * preference left a settings screen saying one address while every sync
     * went to another.
     */
    fun updateRemoteUrl(url: String) {
        val trimmed = url.trim()

        appScope.launch {
            prefs.remoteUrl.update(trimmed)

            // An emptied field means "do not sync", which the preference above
            // already says. Writing it through would ask libgit2 for a remote
            // without an address, and the repository is better left with the
            // one it has than with a broken one.
            if (trimmed.isEmpty()) return@launch

            gitManager.setRemoteUrl(trimmed).onFailure {
                uiHelper.makeToast(it.message)
            }
        }
    }

    fun closeRepo() {
        appScope.launch {
            storageManager.closeRepo()
        }
    }

    fun reloadDatabase() {
        appScope.launch {
            val res = storageManager.updateDatabase(force = true)
            res.onFailure {
                uiHelper.makeToast("$it")
            }
            res.onSuccess {
                uiHelper.makeToast(uiHelper.getString(R.string.success_reload))
            }
        }
    }
}