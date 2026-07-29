package io.github.leonhardweiler.diffusion.ui.model

import android.os.Parcelable
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import kotlinx.parcelize.Parcelize


/**
 * How the remote is authenticated against, which is with an ssh key and nothing
 * else. Username and password used to be the other way; it went with https.
 *
 * There is no username here: the name to log in as is the one standing in the
 * remote url, and the rust side reads it off the url libgit2 is dialling rather
 * than off this. A field carrying "git" for everyone was a repository on a host
 * that answers to another name failing at authentication.
 */
@Parcelize
sealed class Cred : Parcelable {

    data class Ssh(
        val publicKey: String,
        val privateKey: String,
        val passphrase: String?,
    ) : Cred() {
        override fun toString(): String {
            return "Ssh(publicKey=$publicKey, privateKeyLen=${privateKey.length}, passphraseLen=${passphrase?.length})"
        }
    }
}

enum class CredType {
    None,
    Ssh,
}


/**
 * Where the repository lives. Always a folder on the device storage — the app's
 * own private directory was an option once, but a repository nothing else can
 * reach is not what this app is for.
 */
@Parcelize
data class StorageConfiguration(val path: String) : Parcelable {

    fun repoPath(): String = path

    /**
     * Creates the repo directory if it does not exist yet. Never deletes anything:
     * the caller decides what to do with a directory that already has content,
     * using [NodeFs.Folder.isEmptyDirectory].
     */
    fun prepareStorageRepoPath(): Result<Unit> {
        return NodeFs.Folder.fromPath(repoPath()).create()
    }
}
