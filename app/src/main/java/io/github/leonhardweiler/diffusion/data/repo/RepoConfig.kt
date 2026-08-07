package io.github.leonhardweiler.diffusion.data.repo

data class RepoConfig(
    val id: String,
    val path: String,
    val remoteUrl: String = "",
    val sshKeyId: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val syncOnOpenAndClose: Boolean = true,
) {
    val name: String get() = repoNameOf(path)
}

fun repoNameOf(path: String): String =
    path.trimEnd('/').substringAfterLast('/').ifEmpty { path }
