package io.github.leonhardweiler.diffusion.data

import android.content.Context
import io.github.leonhardweiler.diffusion.manager.PreferencesManager
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.CredType
import io.github.leonhardweiler.diffusion.ui.theme.Theme

class AppPreferences(
    context: Context
) : PreferencesManager(context, "settings") {
    companion object {
        const val MAX_REMEMBERED_READING_NOTES = 200
    }

    val theme = enumPreference("theme", Theme.SYSTEM)

    val isInit = booleanPreference("isInit", false)

    private val repoPath = stringPreference("repoPath")

    suspend fun repoPath(): String = repoPath.get()

    val remoteUrl = stringPreference("remoteUrl", "")

    private val credType = enumPreference("credType", CredType.None)

    val gitAuthorName = stringPreference("gitAuthorName", "")
    val gitAuthorEmail = stringPreference("gitAuthorEmail", "")

    private val publicKey = stringPreference("publicKey", "")
    private val privateKey = stringPreference("privateKey", "")
    private val passphrase = stringPreference("passphrase", "")

    suspend fun cred(): Cred? {
        return when (credType.get()) {
            CredType.None -> null

            CredType.Ssh -> Cred.Ssh(
                publicKey = this.publicKey.get(),
                privateKey = this.privateKey.get(),
                passphrase = this.passphrase.get().ifEmpty { null }
            )
        }
    }

    val syncOnOpenAndClose = booleanPreference("syncOnOpenAndClose", true)

    private val readingModeNotes = stringPreference("readingModeNotes")

    private fun noteKey(repoId: String, relativePath: String) = "$repoId/$relativePath"

    fun opensInReadingMode(repoId: String, relativePath: String): Boolean {
        val key = noteKey(repoId, relativePath)
        return readingModeNotes.getBlocking().lineSequence().any { it == key }
    }

    suspend fun setReadingMode(repoId: String, relativePath: String, reading: Boolean) {
        val key = noteKey(repoId, relativePath)

        val kept = readingModeNotes.get()
            .lineSequence()
            .filter { it.isNotEmpty() && it != key }
            .toMutableList()

        if (reading) kept += key

        readingModeNotes.update(
            kept.takeLast(MAX_REMEMBERED_READING_NOTES).joinToString("\n")
        )
    }
}
