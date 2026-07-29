package io.github.leonhardweiler.diffusion.helper

import android.util.Log
import io.github.leonhardweiler.diffusion.R

private const val TAG = "RepoPath"

/**
 * Why a typed name does not name a place in the repository.
 *
 * Each of these is a different thing to do about it, so each of them says which
 * one it is. "Name is invalid" was one sentence for all four, and the one it
 * fitted worst is the one that happens most: a colon in a name, which reads as
 * perfectly ordinary until somebody points at it.
 */
sealed interface PathProblem {

    /** Nothing was typed. */
    data object Empty : PathProblem

    /** What was typed names a folder rather than something in it. */
    data object NamesFolder : PathProblem

    /** A segment holds something a file name cannot hold. */
    data class InvalidCharacter(val character: Char) : PathProblem

    /** More `..` than there is repository above it. */
    data object AboveRoot : PathProblem
}

/** What to say about a path that cannot be given, in words about this one. */
fun PathProblem.describe(uiHelper: UiHelper): String = when (this) {
    PathProblem.Empty -> uiHelper.getString(R.string.error_empty_name)
    PathProblem.NamesFolder -> uiHelper.getString(R.string.error_names_folder)
    PathProblem.AboveRoot -> uiHelper.getString(R.string.error_path_above_root)
    is PathProblem.InvalidCharacter ->
        uiHelper.getString(R.string.error_invalid_character, character.readable())
}

/** A character one can be shown: a tab or a newline has nothing to look at. */
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
    if (trimmed.endsWith("/")) return ResolvedPath.Bad(PathProblem.NamesFolder)

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
                NameValidation.illegalCharacter(segment)?.let {
                    return ResolvedPath.Bad(PathProblem.InvalidCharacter(it))
                }
                segments += segment
            }
        }
    }

    // everything typed resolved away — "." and "work/.." name a folder
    if (segments.isEmpty()) return ResolvedPath.Bad(PathProblem.NamesFolder)

    return ResolvedPath.Ok(segments.joinToString("/"))
}

/**
 * What was typed, with the extension the note already has if none was typed.
 *
 * The extension is part of the name here — typing `notes.txt` over a markdown
 * note is how a note changes type — so a last segment that carries a dot is
 * taken as it stands. One without a dot is a plain name, which is what most
 * renames are, and it keeps what the note is.
 *
 * The dot is looked for in the last segment only: `archive.old/notes` names a
 * note in a folder with a dot in its name, not a note called `old/notes`.
 */
fun keepExtension(typed: String, currentExtension: String): String =
    if (typed.substringAfterLast('/').contains('.') || currentExtension.isEmpty()) {
        typed
    } else {
        "$typed.$currentExtension"
    }

/**
 * The folder a path is in, and the empty string for one that is at the root.
 */
fun getParentPath(path: String) = path.substringBeforeLast(
    delimiter = "/",
    missingDelimiterValue = ""
)

/**
 * A path the rest of the app can compare: no slash at either end.
 *
 * The root folder is the empty string, and everything else is measured from it,
 * so a stray leading slash would make two names for the same place.
 */
fun removeFirstAndLastSlash(input: String): String =
    input.removePrefix("/").removeSuffix("/")

/** Says so in a debug build, where an invariant of the schema is broken. */
fun requireNotEndOrStartWithSlash(str: String) {
    val requirement = !str.startsWith("/") && !str.endsWith("/")
    if (!requirement) {
        Log.d(TAG, "error: requirement not satisfied for $str")
    }
    require(requirement)
}

/**
 * The same place, read from [newPrefix] instead of from [oldPrefix].
 *
 * What a folder move does to every path under it. The prefix is a whole path,
 * so `a/b` moved to `c` turns `a/b/note.md` into `c/note.md` and leaves `a/bc`
 * alone — which is why the caller asks about the folder itself and about paths
 * that begin with it and a slash, and never about the string alone.
 */
fun movedUnder(path: String, oldPrefix: String, newPrefix: String): String =
    newPrefix + path.substring(oldPrefix.length)
