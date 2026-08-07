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

    // Storage work outlives the screen that started it, see AppModule.appScope.
    private val appScope = MyApp.appModule.appScope

    /** The repository whose notes are being looked at. */
    val activeRepo: RepoSession get() = repoManager.active.value

    /** Every repository the app holds, which is what the settings list. */
    val repos: StateFlow<List<RepoSession>> = repoManager.repos

    /** The keys, so that a repository's row can show the one it takes. */
    val sshKeys: Flow<List<StoredSshKey>> = MyApp.appModule.sshKeyStore.keys

    fun repoById(id: String): RepoSession? = repos.value.firstOrNull { it.id == id }

    fun update(f: suspend () -> Unit) {
        viewModelScope.launch {
            f()
        }
    }

    /**
     * Commits, pulls and pushes one repository — the same thing the cloud button
     * above its note list does, for a repository whose note list is not the one
     * on screen.
     */
    fun sync(repo: RepoSession) {
        // Before the coroutine, so the button has changed by the time the finger
        // is lifted. See StorageManager.announceSyncStart.
        repo.storageManager.announceSyncStart()

        appScope.launch {
            repo.open()
            repo.storageManager.syncWithRemote()
        }
    }

    /**
     * Makes another repository the one being looked at, and reads its notes
     * before the screen showing them arrives.
     *
     * @param onChanged called on the main thread once that has happened, so the
     * settings can be left for the note list of the repository that was tapped.
     */
    fun switchTo(repo: RepoSession, onChanged: () -> Unit) {
        appScope.launch {
            val session = repoManager.switchTo(repo.id) ?: return@launch

            session.open()
            session.startShowingItsNotes()

            withContext(Dispatchers.Main) { onChanged() }
        }
    }

    /**
     * Puts a new key pair behind the one this repository names, so that every
     * repository sharing that key gets the new one — which is the point of a
     * key being its own thing.
     *
     * The remote has to be told about it, and until it is, this repository
     * cannot sync. That is what the confirmation in front of this says.
     */
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
     * @param onGone called on the main thread with whether the repository that
     * went was the one being looked at — which decides whether the screen that
     * asked can simply be left or whether the whole app is now standing on
     * another repository.
     */
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
