package io.github.leonhardweiler.gitnote.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.gitnote.MyApp
import io.github.leonhardweiler.gitnote.R
import io.github.leonhardweiler.gitnote.data.AppPreferences
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val storageManager = MyApp.appModule.storageManager
    val uiHelper = MyApp.appModule.uiHelper

    // Storage work outlives the screen that started it, see AppModule.appScope.
    private val appScope = MyApp.appModule.appScope

    fun update(f: suspend () -> Unit) {
        viewModelScope.launch {
            f()
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