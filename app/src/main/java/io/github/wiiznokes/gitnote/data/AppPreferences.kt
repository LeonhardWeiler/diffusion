package io.github.wiiznokes.gitnote.data

import android.content.Context
import io.github.wiiznokes.gitnote.MyApp
import io.github.wiiznokes.gitnote.manager.PreferencesManager
import io.github.wiiznokes.gitnote.provider.ProviderType
import io.github.wiiznokes.gitnote.provider.UserInfo
import io.github.wiiznokes.gitnote.ui.model.Cred
import io.github.wiiznokes.gitnote.ui.model.CredType
import io.github.wiiznokes.gitnote.ui.model.GitAuthor
import io.github.wiiznokes.gitnote.ui.model.StorageConfiguration
import io.github.wiiznokes.gitnote.ui.theme.Theme
import kotlinx.coroutines.runBlocking
import kotlin.io.path.pathString

class AppPreferences(
    context: Context
) : PreferencesManager(context, "settings") {

    companion object {
        const val DEFAULT_USERNAME = "gitnote"
    }

    val dynamicColor = booleanPreference("dynamicColor", true)
    val theme = enumPreference("theme", Theme.SYSTEM)

    val isInit = booleanPreference("isInit", false)
    val databaseCommit = stringPreference("")

    private val repoPath = stringPreference("repoPath")

    suspend fun repoPath(): String {
        if (!isInit.get()) {
            throw Exception("calling repoPath function with no repo initialized")
        }

        return repoPath.get()
    }

    fun repoPathBlocking(): String = runBlocking { repoPath() }



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

    suspend fun applyGitAuthorDefaults(userInfo: UserInfo?, author: GitAuthor?) {
        if (gitAuthorName.get().isEmpty()) {
            (userInfo?.username ?: author?.name)?.let {
                gitAuthorName.update(it)
            }
        }
        if (gitAuthorEmail.get().isEmpty()) {
            (userInfo?.email ?: author?.email)?.let {
                gitAuthorEmail.update(it)
            }
        }
    }

    val userPassUsername = stringPreference("userPassUsername", "")
    val userPassPassword = stringPreference("userPassPassword", "")

    val sshUsername = stringPreference("sshUsername", "")
    val publicKey = stringPreference("publicKey", "")
    val privateKey = stringPreference("privateKey", "")
    val passphrase = stringPreference("passphrase", "")

    val appAuthToken = stringPreference("appAuthToken", "")

    suspend fun cred(): Cred? {
        return when (credType.get()) {
            CredType.None -> null
            CredType.UserPassPlainText -> {
                Cred.UserPassPlainText(
                    username = userPassUsername.get(),
                    password = userPassPassword.get()
                )
            }

            CredType.Ssh -> Cred.Ssh(
                username = this.sshUsername.get(),
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
                sshUsername.update(cred.username)
                publicKey.update(cred.publicKey)
                privateKey.update(cred.privateKey)
                passphrase.update(cred.passphrase ?: "")
            }

            is Cred.UserPassPlainText -> {
                credType.update(CredType.UserPassPlainText)
                userPassUsername.update(cred.username)
                userPassPassword.update(cred.password)
            }

            null -> credType.update(CredType.None)
        }
    }

    val provider = enumPreference("provider", ProviderType.GitHub)

    val defaultExtension = stringPreference("defaultExtension", "md")
    val showLinesNumber = booleanPreference("showLinesNumber", false)


    /**
     * @param remoteUrl what this repository pushes to, empty for one that has no
     * remote. Always written, so that a repository opened after another does not
     * inherit the other one's remote.
     */
    suspend fun initRepo(storageConfig: StorageConfiguration, remoteUrl: String = "") {
        // The index belongs to the repository it was built from, so a different
        // path invalidates it. The same path does not: opening a repository that
        // turns out to have a remote comes back through here after the database
        // was already built, and rebuilding it a second time is the slowest part
        // of a setup that should not be felt at all.
        if (repoPath.get() != storageConfig.path) {
            databaseCommit.update("")
        }
        isInit.update(true)
        this.remoteUrl.update(remoteUrl)

        repoPath.update(storageConfig.path)
    }

    suspend fun closeRepo() {
        isInit.update(false)
    }

    val isReadOnlyModeActive = booleanPreference("isReadOnlyModeActive", false)

}


