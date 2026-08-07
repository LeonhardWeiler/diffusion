package io.github.leonhardweiler.diffusion.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.R
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.data.repo.StoredSshKey
import io.github.leonhardweiler.diffusion.manager.RepoSession
import io.github.leonhardweiler.diffusion.manager.git.generateSshKeys
import io.github.leonhardweiler.diffusion.ui.model.Cred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel : ViewModel() {
    val prefs: AppPreferences = MyApp.appModule.appPreferences
    private val repoManager = MyApp.appModule.repoManager
    private val keyStore = MyApp.appModule.sshKeyStore
    val uiHelper = MyApp.appModule.uiHelper

    private val appScope = MyApp.appModule.appScope

    val activeRepo: RepoSession get() = repoManager.active.value

    val repos: StateFlow<List<RepoSession>> = repoManager.repos

    val sshKeys: Flow<List<StoredSshKey>> = MyApp.appModule.sshKeyStore.keys

    fun repoById(id: String): RepoSession? = repos.value.firstOrNull { it.id == id }

    fun update(f: suspend () -> Unit) {
        viewModelScope.launch {
            f()
        }
    }

    fun sync(repo: RepoSession) {
        repo.storageManager.announceSyncStart()

        appScope.launch {
            repo.open()
            repo.storageManager.syncWithRemote()
        }
    }

    fun switchTo(repo: RepoSession, onChanged: () -> Unit) {
        appScope.launch {
            val session = repoManager.switchTo(repo.id) ?: return@launch

            session.open()
            session.startShowingItsNotes()

            withContext(Dispatchers.Main) { onChanged() }
        }
    }

    fun regenerateSshKey(repo: RepoSession) {
        appScope.launch {
            val (publicKey, privateKey) = generateSshKeys()
            val cred = Cred.Ssh(publicKey = publicKey, privateKey = privateKey, passphrase = null)

            val keyId = repo.prefs.sshKeyId.get()

            if (keyId.isEmpty()) {
                repo.prefs.sshKeyId.update(keyStore.put(cred))
            } else {
                keyStore.replace(keyId, cred)
            }
        }
    }

    fun updateRemoteUrl(repo: RepoSession, url: String) {
        val trimmed = url.trim()

        appScope.launch {
            repo.prefs.remoteUrl.update(trimmed)

            if (trimmed.isEmpty()) return@launch

            repo.gitManager.setRemoteUrl(trimmed).onFailure {
                uiHelper.makeToast(it.message)
            }
        }
    }

    fun removeRepo(repo: RepoSession, onGone: (wasShown: Boolean) -> Unit) {
        val wasShown = repo.id == activeRepo.id

        appScope.launch {
            repoManager.remove(repo.id)?.takeIf { wasShown }?.startShowingItsNotes()

            withContext(Dispatchers.Main) { onGone(wasShown) }
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
