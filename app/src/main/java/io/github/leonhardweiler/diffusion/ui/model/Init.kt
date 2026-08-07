package io.github.leonhardweiler.diffusion.ui.model

import android.os.Parcelable
import io.github.leonhardweiler.diffusion.data.platform.NodeFs
import kotlinx.parcelize.Parcelize

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

@Parcelize
data class StorageConfiguration(val path: String) : Parcelable {
    fun repoPath(): String = path

    fun prepareStorageRepoPath(): Result<Unit> {
        return NodeFs.Folder.fromPath(repoPath()).create()
    }
}
