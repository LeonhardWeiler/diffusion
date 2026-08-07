package io.github.leonhardweiler.diffusion.helper

import android.util.Log
import io.github.leonhardweiler.diffusion.R

private const val TAG = "RepoPath"

sealed interface PathProblem {
    data object Empty : PathProblem

    data object NamesFolder : PathProblem

    data class InvalidCharacter(val character: Char) : PathProblem

    data object AboveRoot : PathProblem
}

fun PathProblem.describe(uiHelper: UiHelper): String = when (this) {
    PathProblem.Empty -> uiHelper.getString(R.string.error_empty_name)
    PathProblem.NamesFolder -> uiHelper.getString(R.string.error_names_folder)
    PathProblem.AboveRoot -> uiHelper.getString(R.string.error_path_above_root)
    is PathProblem.InvalidCharacter ->
        uiHelper.getString(R.string.error_invalid_character, character.readable())
}

private fun Char.readable(): String = when (this) {
    '\n' -> "\\n"
    '\r' -> "\\r"
    '\t' -> "\\t"
    '\u0000', '\u000c' -> code.toString(16).let { "\\u$it" }
    else -> toString()
}

sealed interface ResolvedPath {
    data class Ok(val relativePath: String) : ResolvedPath
    data class Bad(val problem: PathProblem) : ResolvedPath
}

/**
 * What a note is called is a path, not a name: `../notes.md` is a folder up,
 * `archive/notes.md` one beside it, `/notes.md` the root. Read against the
 * folder the thing is in now; the target folder has to exist already, the way
 * mv wants it.
 */
fun resolveRepoPath(parentPath: String, typed: String): ResolvedPath {
    val trimmed = typed.trim()
    if (trimmed.isEmpty()) return ResolvedPath.Bad(PathProblem.Empty)

    if (trimmed.endsWith("/")) return ResolvedPath.Bad(PathProblem.NamesFolder)

    val segments = mutableListOf<String>()

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
                NameValidation.illegalCharacter(segment)?.let {
                    return ResolvedPath.Bad(PathProblem.InvalidCharacter(it))
                }
                segments += segment
            }
        }
    }

    if (segments.isEmpty()) return ResolvedPath.Bad(PathProblem.NamesFolder)

    return ResolvedPath.Ok(segments.joinToString("/"))
}

/** A last segment with no dot keeps the extension the note already has. */
fun keepExtension(typed: String, currentExtension: String): String =
    if (typed.substringAfterLast('/').contains('.') || currentExtension.isEmpty()) {
        typed
    } else {
        "$typed.$currentExtension"
    }

fun getParentPath(path: String) = path.substringBeforeLast(
    delimiter = "/",
    missingDelimiterValue = ""
)

fun removeFirstAndLastSlash(input: String): String =
    input.removePrefix("/").removeSuffix("/")

fun requireNotEndOrStartWithSlash(str: String) {
    val requirement = !str.startsWith("/") && !str.endsWith("/")
    if (!requirement) {
        Log.d(TAG, "error: requirement not satisfied for $str")
    }
    require(requirement)
}

fun movedUnder(path: String, oldPrefix: String, newPrefix: String): String =
    newPrefix + path.substring(oldPrefix.length)
