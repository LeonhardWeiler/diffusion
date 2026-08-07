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

class RepoManager(
    private val store: RepoStore,
    private val keyStore: SshKeyStore,
    private val appPreferences: AppPreferences,
) {
    private val locker = Mutex()

    private val sessions = mutableMapOf<String, RepoSession>()

    private val _repos: MutableStateFlow<List<RepoSession>> = MutableStateFlow(emptyList())

    val repos: StateFlow<List<RepoSession>> = _repos.asStateFlow()

    private val _active: MutableStateFlow<RepoSession> =
        MutableStateFlow(RepoSession.none(store, keyStore))

    val active: StateFlow<RepoSession> = _active.asStateFlow()

    @Volatile
    var loaded = false
        private set

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

    suspend fun openActive(): Boolean {
        val active = _active.value
        if (!active.exists) return false

        if (!active.open()) return false

        active.applyGitAuthorDefaults()
        return true
    }

    suspend fun openTheRest() {
        _repos.value.filter { it.id != _active.value.id }.forEach { it.open() }
    }

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
     * The setup works on a session of its own, and only a setup that goes
     * through hands it over ([adopt]): a url can be wrong, a key can be refused
     * and a clone can be cancelled, and none of those may leave a repository in
     * the list.
     */
    fun draft(path: String): RepoSession =
        RepoSession(id = UUID.randomUUID().toString(), path = path, store = store, keyStore = keyStore)

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

        // a store emptied of its last repository keeps its keys, so setting one
        // up again does not cost the remote another deploy key
        if (kept.isEmpty()) return

        if (kept.any { it.sshKeyId == keyId }) return

        Log.i(TAG, "no repository takes key $keyId anymore")
        keyStore.remove(keyId)
    }

    suspend fun syncAllQuietly() {
        _repos.value.forEach { session ->
            if (!session.syncsOnOpenAndClose()) return@forEach
            if (!session.open()) return@forEach

            session.storageManager.syncWithRemote(announceErrors = false)
        }
    }
}
