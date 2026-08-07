package io.github.leonhardweiler.diffusion.data.repo

/**
 * Everything the app knows about one repository without opening it.
 *
 * There is no display name. A repository is a folder, and what that folder is
 * called is the one name the user already chose for it — a second one, typed in
 * the settings, would be a thing to keep in step with a directory nothing here
 * can rename.
 *
 * @param sshKeyId which of the stored keys this repository authenticates with,
 * empty for one that has no remote to authenticate against. The key itself lives
 * in [SshKeyStore], because two repositories on the same host are usually two
 * repositories with one deploy key.
 */
data class RepoConfig(
    val id: String,
    val path: String,
    val remoteUrl: String = "",
    val sshKeyId: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val syncOnOpenAndClose: Boolean = true,
) {

    /** The folder the repository is in, which is what a row of the list says. */
    val name: String get() = path.trimEnd('/').substringAfterLast('/').ifEmpty { path }
}
