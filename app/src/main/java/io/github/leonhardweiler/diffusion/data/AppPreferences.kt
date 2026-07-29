package io.github.leonhardweiler.diffusion.data

import android.content.Context
import io.github.leonhardweiler.diffusion.manager.PreferencesManager
import io.github.leonhardweiler.diffusion.manager.StringPreference
import io.github.leonhardweiler.diffusion.ui.model.Cred
import io.github.leonhardweiler.diffusion.ui.model.CredType
import io.github.leonhardweiler.diffusion.ui.model.GitAuthor
import io.github.leonhardweiler.diffusion.ui.model.StorageConfiguration
import io.github.leonhardweiler.diffusion.ui.theme.Theme
import kotlinx.coroutines.runBlocking

class AppPreferences(
    context: Context
) : PreferencesManager(context, "settings") {

    companion object {
        const val DEFAULT_USERNAME = "diffusion"
    }

    val theme = enumPreference("theme", Theme.SYSTEM)

    val isInit = booleanPreference("isInit", false)

    private val repoPath = stringPreference("repoPath")

    /**
     * The path as it was last read. It changes when a repository is set up and
     * at no other time, while the editor asks for it on every typing pause —
     * from the main thread, where reading a preference means blocking on a
     * coroutine, which is a stall the user feels as the editor being slow to
     * open and slow to leave.
     */
    @Volatile
    private var knownRepoPath: String? = null

    suspend fun repoPath(): String {
        if (!isInit.get()) {
            throw Exception("calling repoPath function with no repo initialized")
        }

        return knownRepoPath ?: repoPath.get().also { knownRepoPath = it }
    }

    fun repoPathBlocking(): String = knownRepoPath ?: runBlocking { repoPath() }



    val remoteUrl = stringPreference("remoteUrl", "")

    val credType = enumPreference("credType", CredType.None)

    val gitAuthorName = stringPreference("gitAuthorName", "")
    val gitAuthorEmail = stringPreference("gitAuthorEmail", "")

    suspend fun gitAuthor(): GitAuthor {
        return GitAuthor(
            name = gitAuthorName.get().ifEmpty { DEFAULT_USERNAME },
            email = gitAuthorEmail.get()
        )
    }

    suspend fun applyGitAuthorDefaults(author: GitAuthor?) {
        gitAuthorName.fillIn(author?.name)
        gitAuthorEmail.fillIn(author?.email)
    }

    /**
     * Takes the first candidate that says something, unless the preference
     * already does. What the user typed is never overwritten.
     *
     * "null" is not something: an account whose email is private used to arrive
     * as those four letters and be written into commits as an address. Refusing
     * them here is also the only way one that is already stored can be replaced
     * by an address that works.
     */
    private suspend fun StringPreference.fillIn(vararg candidates: String?) {
        if (get().isUsableAuthorField()) return

        candidates.firstOrNull { it.isUsableAuthorField() }?.let { update(it) }
    }

    private fun String?.isUsableAuthorField() = !isNullOrBlank() && this != "null"

    val publicKey = stringPreference("publicKey", "")
    val privateKey = stringPreference("privateKey", "")
    val passphrase = stringPreference("passphrase", "")

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

    suspend fun updateCred(cred: Cred?) {
        when (cred) {
            is Cred.Ssh -> {
                credType.update(CredType.Ssh)
                publicKey.update(cred.publicKey)
                privateKey.update(cred.privateKey)
                passphrase.update(cred.passphrase ?: "")
            }

            null -> credType.update(CredType.None)
        }
    }

    /**
     * Whether the app syncs by itself when it is opened and when it is left.
     *
     * On, because that is what takes the last step off the user — but
     * it is a transfer nobody asked for at the moment it happens, and on mobile
     * data or with a repository one only reads that is a reason to say no.
     */
    val syncOnOpenAndClose = booleanPreference("syncOnOpenAndClose", true)

    /**
     * @param remoteUrl what this repository pushes to, empty for one that has no
     * remote. Always written, so that a repository opened after another does not
     * inherit the other one's remote.
     */
    suspend fun initRepo(storageConfig: StorageConfiguration, remoteUrl: String = "") {
        isInit.update(true)
        this.remoteUrl.update(remoteUrl)

        repoPath.update(storageConfig.path)
        knownRepoPath = storageConfig.path
    }

    suspend fun closeRepo() {
        isInit.update(false)
        knownRepoPath = null
    }

    val isReadOnlyModeActive = booleanPreference("isReadOnlyModeActive", false)

}


