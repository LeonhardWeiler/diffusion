package io.github.leonhardweiler.diffusion.ui.model

/**
 * Who the commits of this repository are written by.
 *
 * Both fields are asked of the repository at setup and can be edited in the
 * settings, and both can be empty — a repository with no `user.name` in its
 * config and no commit to read an author off leaves them so, and the settings
 * let them be cleared.
 *
 * Empty is not something a commit may carry, though. libgit2 refused to write
 * one at all ("Signature cannot have an empty name or email", which upstream
 * saw as `can't commit: -1` and no commit); JGit writes it without complaint,
 * and what comes out is `author  <> 1700000000 +0000` — a history nothing can
 * be attributed in, on a remote shared with other people and other devices. So
 * neither field reaches a commit empty, see [orFallback].
 */
data class GitAuthor(
    val name: String,
    val email: String
) {

    companion object {
        const val DEFAULT_NAME = "diffusion"

        /**
         * Not a domain anybody owns. A made-up address at a real host is mail
         * for somebody who never wrote these notes; `localhost` is what a git
         * with nothing configured writes as well, and it reads as what it is.
         */
        const val DEFAULT_EMAIL = "diffusion@localhost"
    }

    /** The same author, with whatever is missing filled in. */
    fun orFallback(): GitAuthor = GitAuthor(
        name = name.trim().ifEmpty { DEFAULT_NAME },
        email = email.trim().ifEmpty { DEFAULT_EMAIL },
    )
}
