package io.github.wiiznokes.gitnote.ui.model

import android.os.Parcelable
import io.github.wiiznokes.gitnote.data.AppPreferences
import io.github.wiiznokes.gitnote.data.platform.NodeFs
import kotlinx.parcelize.Parcelize


@Parcelize
sealed class Cred : Parcelable {
    data class UserPassPlainText(
        val username: String,
        val password: String
    ) : Cred() {
        override fun toString(): String {
            return "UserPassPlainText(username=$username, password=${"*".repeat(password.length)})"
        }
    }

    data class Ssh(
        val username: String = "git",
        val publicKey: String,
        val privateKey: String,
        val passphrase: String?,
    ) : Cred() {
        override fun toString(): String {
            return "Ssh(username=$username, publicKey=$publicKey, privateKeyLen=${privateKey.length}, passphraseLen=${passphrase?.length})"
        }
    }
}

enum class CredType {
    None,
    UserPassPlainText,
    Ssh,
}


@Parcelize
sealed class StorageConfiguration : Parcelable {
    data object App : StorageConfiguration()
    class Device(val path: String, val useUrlForRootFolder: Boolean = false) :
        StorageConfiguration()

    fun repoPath(): String {
        return when (this) {
            App -> AppPreferences.appStorageRepoPath
            is Device -> this.path
        }
    }

    /**
     * Returns the configuration to actually clone into. When the repo name is
     * taken from the url, that name becomes a sub directory of the chosen path.
     *
     * Returns a new instance instead of mutating, so that a second attempt after
     * a canceled clone does not append the name twice.
     */
    fun withUrlName(url: String): StorageConfiguration {
        if (this !is Device || !useUrlForRootFolder) {
            return this
        }

        val name = url
            .substringAfterLast('/')
            .substringBeforeLast(".git")

        return Device("$path/$name")
    }

    /**
     * Creates the repo directory if it does not exist yet. Never deletes anything:
     * the caller decides what to do with a directory that already has content,
     * using [NodeFs.Folder.isEmptyDirectory].
     */
    fun prepareStorageRepoPath(): Result<Unit> {
        return NodeFs.Folder.fromPath(repoPath()).create()
    }
}
