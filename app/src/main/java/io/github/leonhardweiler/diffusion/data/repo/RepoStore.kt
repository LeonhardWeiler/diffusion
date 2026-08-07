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

class RepoStore(context: Context) : PreferencesManager(context, "repositories") {
    private val repoIds = stringPreference("repoIds")

    private val activeId = stringPreference("activeRepoId")

    private val migrated = booleanPreference("migratedToRepoList", false)

    private val perRepo = mutableMapOf<String, RepoPrefs>()

    @Synchronized
    fun prefsOf(id: String): RepoPrefs = perRepo.getOrPut(id) { RepoPrefs(id) }

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

    val configs: Flow<List<RepoConfig>> = dataStore.data
        .map { preferences -> idsIn(preferences).map { prefsOf(it).read(preferences) } }
        .distinctUntilChanged()

    suspend fun all(): List<RepoConfig> = configs.first()

    suspend fun activeRepoId(): String = activeId.get()

    suspend fun setActive(id: String) = activeId.update(id)

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
