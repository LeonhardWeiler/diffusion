package io.github.leonhardweiler.diffusion.helper

/** Why a typed name does not name a place in the repository. */
enum class PathProblem {
    /** Nothing was typed, or what was typed names a folder rather than a thing in it. */
    Empty,

    /** A segment holds something a file name cannot hold. */
    InvalidName,

    /** More `..` than there is repository above it. */
    AboveRoot,
}

sealed interface ResolvedPath {
    data class Ok(val relativePath: String) : ResolvedPath
    data class Bad(val problem: PathProblem) : ResolvedPath
}

/**
 * Where a typed name lands, read as a path rather than only as a name.
 *
 * Renaming and moving are the same act here: `notes.md` stays where it is,
 * `../notes.md` goes a folder up, `archive/notes.md` goes into a folder beside
 * it, and a leading `/` counts from the root of the repository. That is what
 * `mv` does, and there is nothing else to learn for it.
 *
 * Nothing is created and nothing is checked against the disk — this is the path
 * arithmetic alone, so that it can be reasoned about and tested on its own. Only
 * `..` above the root is refused, because outside the repository is not
 * somewhere a note may go.
 *
 * @param parentPath the folder the thing is in now, relative to the repository
 * root and without leading or trailing slashes.
 */
fun resolveRepoPath(parentPath: String, typed: String): ResolvedPath {
    val trimmed = typed.trim()
    if (trimmed.isEmpty()) return ResolvedPath.Bad(PathProblem.Empty)

    // a trailing slash names the folder, not a thing in it
    if (trimmed.endsWith("/")) return ResolvedPath.Bad(PathProblem.Empty)

    val segments = mutableListOf<String>()

    // a leading slash counts from the root; everything else from where it is
    if (!trimmed.startsWith("/")) {
        segments += parentPath.split('/').filter { it.isNotEmpty() }
    }

    for (segment in trimmed.split('/')) {
        when (segment) {
            "", "." -> continue

            ".." -> {
                if (segments.isEmpty()) return ResolvedPath.Bad(PathProblem.AboveRoot)
                segments.removeAt(segments.size - 1)
            }

            else -> {
                if (!NameValidation.check(segment)) {
                    return ResolvedPath.Bad(PathProblem.InvalidName)
                }
                segments += segment
            }
        }
    }

    if (segments.isEmpty()) return ResolvedPath.Bad(PathProblem.Empty)

    return ResolvedPath.Ok(segments.joinToString("/"))
}
