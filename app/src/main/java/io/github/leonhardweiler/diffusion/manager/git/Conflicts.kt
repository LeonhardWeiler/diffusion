package io.github.leonhardweiler.diffusion.manager.git

import org.eclipse.jgit.lib.Repository
import java.io.File

// never "=======", which is how markdown underlines a heading
private val CONFLICT_MARKERS = listOf("<<<<<<<", ">>>>>>>")

internal fun unresolvedConflicts(repo: Repository): List<String> {
    val workTree = repo.workTree ?: return emptyList()

    return conflictedPaths(repo)
        .filter { path -> holdsMarkers(File(workTree, path)) }
        .distinct()
        .sorted()
}

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

private fun holdsMarkers(file: File): Boolean = runCatching {
    file.useLines { lines ->
        lines.any { line -> CONFLICT_MARKERS.any { line.startsWith(it) } }
    }
}.getOrDefault(false)
