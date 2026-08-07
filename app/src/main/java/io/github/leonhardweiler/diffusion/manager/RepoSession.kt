package io.github.leonhardweiler.diffusion.manager

import android.util.Log
import io.github.leonhardweiler.diffusion.data.index.NoteIndex
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.data.repo.RepoStore
import io.github.leonhardweiler.diffusion.data.repo.SshKeyStore
import io.github.leonhardweiler.diffusion.data.repo.repoNameOf
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.GitAuthor

private const val TAG = "RepoSession"

class RepoSession(
    val id: String,
    val path: String,
    private val store: RepoStore,
    private val keyStore: SshKeyStore,
) {
    companion object {
        /**
         * Stands in while there is no repository, so the note list, the editor
         * and the sync button keep working on a session rather than on a null.
         */
        fun none(store: RepoStore, keyStore: SshKeyStore) =
            RepoSession(id = "", path = "", store = store, keyStore = keyStore)
    }

    val exists: Boolean get() = id.isNotEmpty()

    val name: String get() = repoNameOf(path)

    val prefs: RepoStore.RepoPrefs = store.prefsOf(id)

    val noteIndex: NoteIndex = NoteIndex()

    val gitManager: GitManager = GitManager()

    val storageManager: StorageManager by lazy { StorageManager(this) }

    /**
     * Every repository is opened — refs and config, which is cheap — but only
     * the one being looked at reads its working tree. A pull into another one
     * writes files and stops there; switching to it reads it then.
     */
    @Volatile
    var showsItsNotes: Boolean = false

    suspend fun remoteUrl(): String = prefs.remoteUrl.get()

    suspend fun syncsOnOpenAndClose(): Boolean = prefs.syncOnOpenAndClose.get()

    suspend fun gitAuthor(): GitAuthor = GitAuthor(
        name = prefs.authorName.get(),
        email = prefs.authorEmail.get(),
    )

    suspend fun cred(): Cred? = keyStore.get(prefs.sshKeyId.get())?.cred()

    suspend fun open(): Boolean {
        if (!exists) return false
        if (gitManager.isRepoInitialized) return true

        if (!NodeFs.Folder.fromPath(path).exist()) {
            Log.w(TAG, "$id: $path is not there")
            return false
        }

        return gitManager.openRepo(path).isSuccess
    }

    suspend fun close() {
        gitManager.closeRepo()
        noteIndex.clear()
        showsItsNotes = false
    }

    suspend fun startShowingItsNotes(progressCb: ((Progress) -> Unit)? = null) {
        showsItsNotes = true

        storageManager.rebuildIndex(progressCb)

        storageManager.refreshChangeState()
    }

    suspend fun applyGitAuthorDefaults() {
        val signature = gitManager.currentSignature() ?: return

        if (!prefs.authorName.get().isUsable()) {
            signature.name.takeIf { it.isUsable() }?.let { prefs.authorName.update(it) }
        }

        if (!prefs.authorEmail.get().isUsable()) {
            signature.email.takeIf { it.isUsable() }?.let { prefs.authorEmail.update(it) }
        }
    }

    private fun String?.isUsable() = !isNullOrBlank() && this != "null"

    override fun toString(): String = "RepoSession($id, $path)"
}
