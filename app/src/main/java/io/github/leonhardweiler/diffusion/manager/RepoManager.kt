package io.github.leonhardweiler.diffusion.manager

import android.util.Log
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.data.repo.RepoConfig
import io.github.leonhardweiler.diffusion.data.repo.RepoStore
import io.github.leonhardweiler.diffusion.data.repo.SshKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private const val TAG = "RepoManager"

/**
 * Every repository the app holds, and which of them the note list is showing.
 *
 * One session per configured repository, all of them opened, exactly one of them
 * showing its notes. Switching is the only thing that reads a working tree, and
 * it is a thing done in the settings — so the screen it leads to is built again
 * from scratch, which is why [io.github.leonhardweiler.diffusion.ui.destination.Destination.App]
 * carries the repository's id.
 *
 * [active] never holds null. A session that is no repository stands in while
 * there is none, so that the note list, the editor and the sync button keep
 * working on something rather than on a null they would all have to ask about.
 */
class RepoManager(
    private val store: RepoStore,
    private val keyStore: SshKeyStore,
    private val appPreferences: AppPreferences,
) {

    private val locker = Mutex()

    private val sessions = mutableMapOf<String, RepoSession>()

    private val _repos: MutableStateFlow<List<RepoSession>> = MutableStateFlow(emptyList())

    /** Every repository, in the order they were set up. */
    val repos: StateFlow<List<RepoSession>> = _repos.asStateFlow()

    private val _active: MutableStateFlow<RepoSession> =
        MutableStateFlow(RepoSession.none(store, keyStore))

    /** The repository the note list, the search and the editor are about. */
    val active: StateFlow<RepoSession> = _active.asStateFlow()

    /**
     * Whether the store has been read yet. The activity asks for this on the way
     * to its first frame, at every creation of it — a rotation must not walk the
     * preferences again.
     */
    @Volatile
    var loaded = false
        private set

    /**
     * Reads the repositories out of the store and opens them, once per process.
     *
     * The one that was last looked at is the one that comes back, and only that
     * one is asked for its notes.
     */
    suspend fun load(): List<RepoSession> = locker.withLock {
        if (loaded) return@withLock _repos.value

        store.migrateFrom(appPreferences, keyStore)

        val configs = store.all()
        val activeId = store.activeRepoId()

        _repos.value = configs.map { sessionFor(it) }

        val active = _repos.value.firstOrNull { it.id == activeId } ?: _repos.value.firstOrNull()
        active?.let { _active.value = it }

        loaded = true
        Log.i(TAG, "loaded ${configs.size} repositories, showing ${active?.id}")

        _repos.value
    }

    private fun sessionFor(config: RepoConfig): RepoSession = sessions.getOrPut(config.id) {
        RepoSession(id = config.id, path = config.path, store = store, keyStore = keyStore)
    }

    /**
     * Opens the repository being looked at and builds its note list; the others
     * are only opened, because nothing is drawing them.
     *
     * Answers with whether there is a repository showing its notes at all, which
     * is what decides between the note list and the setup.
     */
    suspend fun openActive(): Boolean {
        val active = _active.value
        if (!active.exists) return false

        if (!active.open()) return false

        active.applyGitAuthorDefaults()
        return true
    }

    /** Opens every repository other than the one being looked at, quietly. */
    suspend fun openTheRest() {
        _repos.value.filter { it.id != _active.value.id }.forEach { it.open() }
    }

    /**
     * Makes [id] the repository being looked at.
     *
     * The one being left keeps its open git repository — its button in the
     * settings still says whether it has anything to send — and lets go of the
     * note list nobody is drawing anymore.
     */
    suspend fun switchTo(id: String): RepoSession? = locker.withLock {
        val session = _repos.value.firstOrNull { it.id == id } ?: return@withLock null
        if (session.id == _active.value.id) return@withLock session

        _active.value.let {
            it.showsItsNotes = false
            it.noteIndex.clear()
        }

        store.setActive(id)
        _active.value = session

        session
    }

    /**
     * A session for a repository that is being set up and is not one yet.
     *
     * The setup opens or clones into a folder long before it knows whether that
     * will end in a repository — the url can be wrong, the key can be refused,
     * the clone can be cancelled — and all of that happens through a git
     * repository that has to be open. So it works on a session of its own, and
     * only [adopt] puts it in the list.
     */
    fun draft(path: String): RepoSession =
        RepoSession(id = UUID.randomUUID().toString(), path = path, store = store, keyStore = keyStore)

    /**
     * Takes a session the setup has finished with into the list, and makes it
     * the one being looked at — the setup ends on its notes.
     */
    suspend fun adopt(session: RepoSession, config: RepoConfig): RepoSession = locker.withLock {
        store.add(config.copy(id = session.id, path = session.path))

        sessions[session.id] = session
        _repos.value = _repos.value.filter { it.id != session.id } + session

        _active.value.takeIf { it.id != session.id }?.let {
            it.showsItsNotes = false
            it.noteIndex.clear()
        }

        store.setActive(session.id)
        _active.value = session

        session
    }

    /**
     * Lets go of a repository. Nothing on disk is touched — the folder and its
     * notes stay exactly where they are.
     *
     * The key it used goes with it when no other repository takes that key and
     * there is another repository at all: a store emptied of its last repository
     * keeps its keys, so that setting one up again does not cost the remote
     * another deploy key.
     *
     * @return the repository being looked at now, or null when that was the last
     * one and there is nothing left to look at.
     */
    suspend fun remove(id: String): RepoSession? = locker.withLock {
        sessions.remove(id)?.let { going ->
            val keyId = going.prefs.sshKeyId.get()
            going.close()

            val kept = store.remove(id)
            _repos.value = kept.mapNotNull { sessions[it.id] }

            pruneKey(keyId, kept)
        }

        if (_active.value.id == id) {
            _active.value = _repos.value.firstOrNull() ?: RepoSession.none(store, keyStore)
        }

        _active.value.takeIf { it.exists }
    }

    private suspend fun pruneKey(keyId: String, kept: List<RepoConfig>) {
        if (keyId.isEmpty()) return

        // A store with no repository left is a store on its way to a new setup,
        // and the key it still holds is the one that setup would otherwise ask
        // the remote for again.
        if (kept.isEmpty()) return

        if (kept.any { it.sshKeyId == keyId }) return

        Log.i(TAG, "no repository takes key $keyId anymore")
        keyStore.remove(keyId)
    }

    /**
     * Commits, pulls and pushes every repository that was told to do so by
     * itself. What the app does when it is opened and when it is left.
     */
    suspend fun syncAllQuietly() {
        _repos.value.forEach { session ->
            if (!session.syncsOnOpenAndClose()) return@forEach
            if (!session.open()) return@forEach

            session.storageManager.syncWithRemote(announceErrors = false)
        }
    }
}
