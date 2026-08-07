package io.github.leonhardweiler.diffusion.manager.git

import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.util.io.DisabledOutputStream

private const val MAX_NAMES_IN_SUBJECT = 3

internal data class Changes(
    val added: List<String> = emptyList(),
    val changed: List<String> = emptyList(),
    val deleted: List<String> = emptyList(),
) {
    fun isEmpty(): Boolean = added.isEmpty() && changed.isEmpty() && deleted.isEmpty()

    fun groups(): List<Pair<String, List<String>>> =
        listOf("added" to added, "changed" to changed, "deleted" to deleted)

    companion object {
        fun of(repo: Repository): Changes {
            val added = mutableListOf<String>()
            val changed = mutableListOf<String>()
            val deleted = mutableListOf<String>()

            for (entry in indexAgainstHead(repo)) {
                when (entry.changeType) {
                    DiffEntry.ChangeType.ADD, DiffEntry.ChangeType.COPY ->
                        added += entry.newPath

                    DiffEntry.ChangeType.DELETE ->
                        deleted += entry.oldPath

                    DiffEntry.ChangeType.MODIFY, DiffEntry.ChangeType.RENAME ->
                        changed += entry.newPath

                    null -> Unit
                }
            }

            return Changes(added.sorted(), changed.sorted(), deleted.sorted())
        }

        private fun indexAgainstHead(repo: Repository): List<DiffEntry> =
            repo.newObjectReader().use { reader ->
                DiffFormatter(DisabledOutputStream.INSTANCE).use { formatter ->
                    formatter.setRepository(repo)
                    formatter.setDetectRenames(false)

                    formatter.scan(headTree(repo, reader), DirCacheIterator(repo.readDirCache()))
                }
            }

        private fun headTree(repo: Repository, reader: ObjectReader): AbstractTreeIterator {
            val head = repo.resolve("${Constants.HEAD}^{tree}") ?: return EmptyTreeIterator()
            return CanonicalTreeParser(null, reader, head)
        }
    }
}

internal fun subjectGroup(paths: List<String>, verb: String): String? {
    if (paths.isEmpty()) return null

    val list = buildString {
        append(paths.take(MAX_NAMES_IN_SUBJECT).joinToString(", "))

        val hidden = paths.size - MAX_NAMES_IN_SUBJECT
        if (hidden > 0) append(" and $hidden more")
    }

    return "[$list] $verb"
}

/**
 * `[fresh.md] added, [kept.md] changed, [gone.md] deleted` — the first
 * [MAX_NAMES_IN_SUBJECT] of a group in the subject, the rest counted there and
 * listed underneath. [fallback] is only for a commit with nothing to name.
 */
internal fun commitMessage(changes: Changes, merging: Boolean, fallback: String): String {
    if (changes.isEmpty()) {
        return if (merging) "Merge" else fallback
    }

    val subject = changes.groups()
        .mapNotNull { (verb, paths) -> subjectGroup(paths, verb) }
        .joinToString(", ")

    if (changes.groups().all { (_, paths) -> paths.size <= MAX_NAMES_IN_SUBJECT }) {
        return subject
    }

    val body = changes.groups()
        .filter { (_, paths) -> paths.isNotEmpty() }
        .joinToString("\n\n") { (verb, paths) ->
            "$verb:\n" + paths.joinToString("\n") { "  $it" }
        }

    return "$subject\n\n$body"
}

internal fun commitMessage(repo: Repository, fallback: String): String = commitMessage(
    changes = Changes.of(repo),
    merging = repo.isMerging(),
    fallback = fallback,
)

internal fun Repository.isMerging(): Boolean = repositoryState.let {
    it == RepositoryState.MERGING || it == RepositoryState.MERGING_RESOLVED
}
