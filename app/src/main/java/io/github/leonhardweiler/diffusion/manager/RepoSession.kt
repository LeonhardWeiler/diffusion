package io.github.leonhardweiler.diffusion.manager

import android.util.Log
import io.github.leonhardweiler.diffusion.MyApp
import io.github.leonhardweiler.diffusion.data.index.NoteIndex
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import io.github.leonhardweiler.diffusion.data.repo.RepoConfig
import io.github.leonhardweiler.diffusion.data.repo.RepoStore
import io.github.leonhardweiler.diffusion.data.repo.SshKeyStore
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.GitAuthor

private const val TAG = "RepoSession"

/**
 * One repository, with everything that belongs to it and to nothing else: the
 * open git repository, the note index built from its files, the write path into
 * both, and its own sync state.
 *
 * All of that used to be one of each in [io.github.leonhardweiler.diffusion.AppModule],
 * because there was one repository. What made it into a session rather than a
 * bundle of singletons is the cloud button beside every row of the settings: a
 * repository that is not the one being looked at still commits, pulls, pushes
 * and carries a dot for what it has not sent, and each of those answers about
 * itself.
 *
 * The path is a val and everything else is asked for when it is needed. A
 * repository cannot move — the folder is where it is — while its remote, its
 * author and its key are all things the settings screen changes while the
 * session is open.
 */
class RepoSession(
    val id: String,
    val path: String,
    private val store: RepoStore,
    private val keyStore: SshKeyStore,
) {

    companion object {
        /**
         * What the app has open when it has no repository: an id that is no
         * repository's, a path that is nowhere, an index that is empty and a git
         * repository that was never opened.
         *
         * So that everything downstream — the note list, the editor, the sync
         * button — keeps working on a session rather than on a null. What they
         * then show is an empty list and a sync that does nothing, which is
         * exactly what the app showed after the last repository was closed.
         */
        fun none(store: RepoStore, keyStore: SshKeyStore) =
            RepoSession(id = "", path = "", store = store, keyStore = keyStore)
    }

    /** Whether this is a repository at all, rather than the placeholder above. */
    val exists: Boolean get() = id.isNotEmpty()

    val prefs: RepoStore.RepoPrefs = store.prefsOf(id)

    val noteIndex: NoteIndex = NoteIndex()

    val gitManager: GitManager = GitManager()

    /**
     * Lazily, because it reads this session while this session is still being
     * built — and because a repository that is only listed in the settings never
     * needs a write path at all.
     */
    val storageManager: StorageManager by lazy { StorageManager(this) }

    /**
     * Whether the note list is being built from this repository.
     *
     * Only the repository being looked at has one. A pull into another one
     * writes files nothing is showing, and walking its whole working tree to
     * build a list nobody is drawing is the one expensive thing a background
     * sync could do. Switching to a repository reads it then.
     */
    @Volatile
    var showsItsNotes: Boolean = false

    suspend fun remoteUrl(): String = prefs.remoteUrl.get()

    suspend fun syncsOnOpenAndClose(): Boolean = prefs.syncOnOpenAndClose.get()

    /**
     * Who to write the commits as. What is missing is filled in where it reaches
     * git ([GitAuthor.orFallback]) rather than here — the settings show these two
     * as they are stored, and "none" is the honest word for a field nobody has
     * filled in.
     */
    suspend fun gitAuthor(): GitAuthor = GitAuthor(
        name = prefs.authorName.get(),
        email = prefs.authorEmail.get(),
    )

    /** The key this repository was set up with, or none for one without a remote. */
    suspend fun cred(): Cred? = keyStore.get(prefs.sshKeyId.get())?.cred()

    /**
     * Opens the git repository if it is not open yet, and says whether there is
     * one to work with.
     *
     * Every repository is opened, not only the one being looked at: the others
     * still sync, and their button in the settings says how that went. Opening
     * is cheap — refs and config — while reading the working tree is not, and
     * that is [showsItsNotes].
     */
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

    /**
     * Reads the whole working tree into this session's index, for the one
     * moment it is worth it: this repository has just become the one being
     * looked at.
     */
    suspend fun startShowingItsNotes(progressCb: ((Progress) -> Unit)? = null) {
        showsItsNotes = true

        storageManager.rebuildIndex(progressCb)

        // What a previous run left in the working tree is still there — being
        // killed does not commit anything, and the flag behind the dot starts
        // false in a fresh process.
        storageManager.refreshChangeState()
    }

    /**
     * Applies the author this device would otherwise have made up, from whatever
     * the repository itself already says.
     */
    suspend fun applyGitAuthorDefaults() {
        val signature = gitManager.currentSignature() ?: return

        if (!prefs.authorName.get().isUsable()) {
            signature.name.takeIf { it.isUsable() }?.let { prefs.authorName.update(it) }
        }

        if (!prefs.authorEmail.get().isUsable()) {
            signature.email.takeIf { it.isUsable() }?.let { prefs.authorEmail.update(it) }
        }
    }

    /**
     * What the user typed is never overwritten, and "null" is not something: an
     * account whose email is private used to arrive as those four letters and be
     * written into commits as an address.
     */
    private fun String?.isUsable() = !isNullOrBlank() && this != "null"

    override fun toString(): String = "RepoSession($id, $path)"
}
