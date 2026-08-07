package io.github.leonhardweiler.diffusion.ui.model

data class GitAuthor(
    val name: String,
    val email: String
) {
    companion object {
        const val DEFAULT_NAME = "diffusion"

        const val DEFAULT_EMAIL = "diffusion@localhost"
    }

    /**
     * Applied where the author reaches git, not where it comes from: the
     * settings show what is stored, and "none" is the honest word for a field
     * nobody has filled in. JGit writes `author  <> …` without complaint.
     */
    fun orFallback(): GitAuthor = GitAuthor(
        name = name.trim().ifEmpty { DEFAULT_NAME },
        email = email.trim().ifEmpty { DEFAULT_EMAIL },
    )
}
