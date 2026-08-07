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

/** One key pair the app holds, and the id the repositories name it by. */
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

/**
 * The ssh keys, apart from the repositories that use them.
 *
 * They used to be three fields of the one repository there was, which is why
 * setting up a second one meant generating a second key and adding a second
 * deploy key for it. Two repositories on the same host are usually two
 * repositories behind one key, so a key is its own thing here and a repository
 * only says which one it takes.
 *
 * Ids are held as one newline-separated line rather than as a serialised list:
 * a key is a handful of strings, DataStore already stores strings, and a format
 * of our own would be one more thing to keep readable across versions.
 */
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

    /** Every key there is, in the order they were added. */
    val keys: Flow<List<StoredSshKey>> = dataStore.data
        .map { preferences -> idsIn(preferences).map { prefsOf(it).read(preferences) } }
        .distinctUntilChanged()

    suspend fun all(): List<StoredSshKey> = keys.first()

    suspend fun get(id: String): StoredSshKey? =
        if (id.isEmpty()) null else all().firstOrNull { it.id == id }

    /**
     * Writes a key down and answers with the id it is known by.
     *
     * A pair whose public half is already here keeps the id it already had, and
     * only its private half and passphrase are written again. Otherwise setting
     * a second repository up with the same deploy key would leave two entries
     * saying the same thing, and the screen offering them would ask which of two
     * identical keys to take.
     */
    suspend fun put(cred: Cred.Ssh): String {
        val current = all()
        val existing = current.firstOrNull { it.publicKey.trim() == cred.publicKey.trim() }

        val id = existing?.id ?: UUID.randomUUID().toString()
        val ids = if (existing == null) current.map { it.id } + id else current.map { it.id }

        write(id, cred, ids)
        return id
    }

    /**
     * Puts another pair behind an id that is already in use.
     *
     * For the one thing that happens to a key after it exists: it is
     * regenerated. The repositories point at the id, so replacing what stands
     * behind it is what leaves them all with the new key.
     */
    suspend fun replace(id: String, cred: Cred.Ssh) {
        val ids = all().map { it.id }
        if (id !in ids) return

        write(id, cred, ids)
    }

    /** One edit for both halves, so nothing is ever listed without its key. */
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
