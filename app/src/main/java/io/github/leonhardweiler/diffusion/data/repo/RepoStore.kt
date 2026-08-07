package io.github.leonhardweiler.diffusion.data.repo

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import io.github.leonhardweiler.diffusion.data.AppPreferences
import io.github.leonhardweiler.diffusion.manager.BooleanPreference
import io.github.leonhardweiler.diffusion.manager.PreferencesManager
import io.github.leonhardweiler.diffusion.manager.PreferencesManager.Companion.editor
import io.github.leonhardweiler.diffusion.manager.StringPreference
import io.github.leonhardweiler.diffusion.ui.model.Cred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private const val TAG = "RepoStore"

/**
 * The repositories the app knows about, and which of them is being looked at.
 *
 * Everything a repository carries is a preference keyed by its id — there is no
 * serialised blob here. A repository is six values, DataStore stores exactly
 * those types, and the per-repository settings screen wants each of them as its
 * own flow anyway ([RepoPrefs]).
 */
class RepoStore(context: Context) : PreferencesManager(context, "repositories") {

    private val repoIds = stringPreference("repoIds")

    private val activeId = stringPreference("activeRepoId")

    /**
     * Whether the one repository the app used to hold has been carried over.
     * Written even when there was nothing to carry, so that a user who removes
     * every repository is not handed the old one back at the next start.
     */
    private val migrated = booleanPreference("migratedToRepoList", false)

    private val perRepo = mutableMapOf<String, RepoPrefs>()

    @Synchronized
    fun prefsOf(id: String): RepoPrefs = perRepo.getOrPut(id) { RepoPrefs(id) }

    /**
     * What one repository is made of, as preferences rather than as a value: the
     * settings screen of a repository shows each of these and writes each of
     * them on its own.
     */
    inner class RepoPrefs(val id: String) {
        val path: StringPreference = stringPreference("repo.$id.path")
        val remoteUrl: StringPreference = stringPreference("repo.$id.remoteUrl")
        val sshKeyId: StringPreference = stringPreference("repo.$id.sshKeyId")
        val authorName: StringPreference = stringPreference("repo.$id.authorName")
        val authorEmail: StringPreference = stringPreference("repo.$id.authorEmail")
        val syncOnOpenAndClose: BooleanPreference =
            booleanPreference("repo.$id.syncOnOpenAndClose", true)

        internal fun read(preferences: Preferences) = RepoConfig(
            id = id,
            path = path.valueIn(preferences),
            remoteUrl = remoteUrl.valueIn(preferences),
            sshKeyId = sshKeyId.valueIn(preferences),
            authorName = authorName.valueIn(preferences),
            authorEmail = authorEmail.valueIn(preferences),
            syncOnOpenAndClose = syncOnOpenAndClose.valueIn(preferences),
        )
    }

    private fun idsIn(preferences: Preferences): List<String> =
        repoIds.valueIn(preferences).lineSequence().filter { it.isNotEmpty() }.toList()

    /** Every repository, in the order they were set up. */
    val configs: Flow<List<RepoConfig>> = dataStore.data
        .map { preferences -> idsIn(preferences).map { prefsOf(it).read(preferences) } }
        .distinctUntilChanged()

    suspend fun all(): List<RepoConfig> = configs.first()

    suspend fun activeRepoId(): String = activeId.get()

    suspend fun setActive(id: String) = activeId.update(id)

    /**
     * Writes a repository down and answers with the id it is known by. The first
     * one to be added is also the one being looked at, so that setting up an app
     * that had none ends on its note list.
     */
    suspend fun add(config: RepoConfig): String {
        val id = config.id.ifEmpty { UUID.randomUUID().toString() }
        val current = all()
        val ids = if (current.any { it.id == id }) current.map { it.id } else current.map { it.id } + id

        val prefs = prefsOf(id)
        dataStore.editor {
            prefs.path.value = config.path
            prefs.remoteUrl.value = config.remoteUrl
            prefs.sshKeyId.value = config.sshKeyId
            prefs.authorName.value = config.authorName
            prefs.authorEmail.value = config.authorEmail
            prefs.syncOnOpenAndClose.value = config.syncOnOpenAndClose
            repoIds.value = ids.joinToString("\n")

            if (current.isEmpty()) activeId.value = id
        }

        return id
    }

    /**
     * Forgets a repository, and hands back what is left so the caller can decide
     * which of them to look at now. Nothing on disk is touched: removing a
     * repository here is the app letting go of a folder, not the folder going
     * away.
     */
    suspend fun remove(id: String): List<RepoConfig> {
        val kept = all().filter { it.id != id }
        val prefs = prefsOf(id)
        val wasActive = activeRepoId() == id

        dataStore.editor {
            prefs.path.forget()
            prefs.remoteUrl.forget()
            prefs.sshKeyId.forget()
            prefs.authorName.forget()
            prefs.authorEmail.forget()
            prefs.syncOnOpenAndClose.forget()
            repoIds.value = kept.joinToString("\n") { it.id }

            if (wasActive) activeId.value = kept.firstOrNull()?.id.orEmpty()
        }

        return kept
    }

    /**
     * Carries the one repository the app used to hold into the list, once.
     *
     * Everything about it was a preference of its own — the path, the remote,
     * the key, the author — because there was only ever one of it. The old
     * values are left where they are rather than deleted: they cost a few
     * hundred bytes, and a build that is rolled back still finds its repository.
     */
    suspend fun migrateFrom(appPreferences: AppPreferences, keyStore: SshKeyStore) {
        if (migrated.get()) return

        migrated.update(true)

        if (!appPreferences.isInit.get()) return

        val path = runCatching { appPreferences.repoPath() }.getOrNull().orEmpty()
        if (path.isEmpty()) return

        val keyId = (appPreferences.cred() as? Cred.Ssh)
            ?.takeIf { it.publicKey.isNotEmpty() && it.privateKey.isNotEmpty() }
            ?.let { keyStore.put(it) }
            .orEmpty()

        val id = add(
            RepoConfig(
                id = UUID.randomUUID().toString(),
                path = path,
                remoteUrl = appPreferences.remoteUrl.get(),
                sshKeyId = keyId,
                authorName = appPreferences.gitAuthorName.get(),
                authorEmail = appPreferences.gitAuthorEmail.get(),
                syncOnOpenAndClose = appPreferences.syncOnOpenAndClose.get(),
            )
        )

        Log.i(TAG, "carried the single repository over as $id")
    }
}
