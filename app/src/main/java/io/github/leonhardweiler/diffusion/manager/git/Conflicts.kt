package io.github.leonhardweiler.diffusion.manager.git

import org.eclipse.jgit.lib.Repository
import java.io.File

/**
 * The lines a conflict leaves behind that no note would hold otherwise.
 *
 * "=======" is not among them: seven equals signs at the start of a line are how
 * markdown underlines a heading, and a note that has one is not a note with a
 * conflict in it.
 */
private val CONFLICT_MARKERS = listOf("<<<<<<<", ">>>>>>>")

/**
 * The notes still holding the markers a conflict wrote into them.
 *
 * Only the ones the merge could not merge are looked at: they are the only ones
 * a marker could have got into, and reading the whole repository to find that
 * out would be paid for on every sync.
 */
internal fun unresolvedConflicts(repo: Repository): List<String> {
    val workTree = repo.workTree ?: return emptyList()

    return conflictedPaths(repo)
        .filter { path -> holdsMarkers(File(workTree, path)) }
        .distinct()
        .sorted()
}

/** What the index still lists as conflicted, which is what a merge left open. */
internal fun conflictedPaths(repo: Repository): List<String> {
    val index = runCatching { repo.readDirCache() }.getOrNull() ?: return emptyList()

    return (0 until index.entryCount)
        .asSequence()
        .map { index.getEntry(it) }
        .filter { it.stage != 0 }
        .map { it.pathString }
        .distinct()
        .toList()
}

/**
 * A note that was deleted rather than edited resolved the conflict as well, and
 * reading it is how that is noticed.
 */
private fun holdsMarkers(file: File): Boolean = runCatching {
    file.useLines { lines ->
        lines.any { line -> CONFLICT_MARKERS.any { line.startsWith(it) } }
    }
}.getOrDefault(false)
