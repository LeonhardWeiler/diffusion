package io.github.leonhardweiler.diffusion.data.repo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import io.github.leonhardweiler.diffusion.manager.PreferencesManager
import io.github.leonhardweiler.diffusion.manager.PreferencesManager.Companion.editor
import io.github.leonhardweiler.diffusion.ui.model.Cred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

data class StoredSshKey(
    val id: String,
    val publicKey: String,
    val privateKey: String,
    val passphrase: String?,
) {
    fun cred(): Cred.Ssh = Cred.Ssh(
        publicKey = publicKey,
        privateKey = privateKey,
        passphrase = passphrase?.ifEmpty { null },
    )
}

class SshKeyStore(context: Context) : PreferencesManager(context, "ssh_keys") {
    private val keyIds = stringPreference("sshKeyIds")

    private val perKey = mutableMapOf<String, KeyPrefs>()

    @Synchronized
    private fun prefsOf(id: String): KeyPrefs = perKey.getOrPut(id) { KeyPrefs(id) }

    private inner class KeyPrefs(val id: String) {
        val publicKey = stringPreference("sshKey.$id.public")
        val privateKey = stringPreference("sshKey.$id.private")
        val passphrase = stringPreference("sshKey.$id.passphrase")

        fun read(preferences: Preferences) = StoredSshKey(
            id = id,
            publicKey = publicKey.valueIn(preferences),
            privateKey = privateKey.valueIn(preferences),
            passphrase = passphrase.valueIn(preferences).ifEmpty { null },
        )
    }

    private fun idsIn(preferences: Preferences): List<String> =
        keyIds.valueIn(preferences).lineSequence().filter { it.isNotEmpty() }.toList()

    val keys: Flow<List<StoredSshKey>> = dataStore.data
        .map { preferences -> idsIn(preferences).map { prefsOf(it).read(preferences) } }
        .distinctUntilChanged()

    suspend fun all(): List<StoredSshKey> = keys.first()

    suspend fun get(id: String): StoredSshKey? =
        if (id.isEmpty()) null else all().firstOrNull { it.id == id }

    suspend fun put(cred: Cred.Ssh): String {
        val current = all()
        val existing = current.firstOrNull { it.publicKey.trim() == cred.publicKey.trim() }

        val id = existing?.id ?: UUID.randomUUID().toString()
        val ids = if (existing == null) current.map { it.id } + id else current.map { it.id }

        write(id, cred, ids)
        return id
    }

    suspend fun replace(id: String, cred: Cred.Ssh) {
        val ids = all().map { it.id }
        if (id !in ids) return

        write(id, cred, ids)
    }

    private suspend fun write(id: String, cred: Cred.Ssh, ids: List<String>) {
        val prefs = prefsOf(id)
        dataStore.editor {
            prefs.publicKey.value = cred.publicKey
            prefs.privateKey.value = cred.privateKey
            prefs.passphrase.value = cred.passphrase.orEmpty()
            keyIds.value = ids.joinToString("\n")
        }
    }

    suspend fun remove(id: String) {
        val prefs = prefsOf(id)
        val kept = all().map { it.id }.filter { it != id }

        dataStore.editor {
            prefs.publicKey.forget()
            prefs.privateKey.forget()
            prefs.passphrase.forget()
            keyIds.value = kept.joinToString("\n")
        }
    }
}
