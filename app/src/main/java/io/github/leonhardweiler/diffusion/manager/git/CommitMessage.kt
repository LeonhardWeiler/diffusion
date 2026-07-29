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

/**
 * How many names a group of the subject line carries before the rest of them
 * are only counted.
 */
private const val MAX_NAMES_IN_SUBJECT = 3

/** The notes a commit is about, grouped by what happened to them. */
internal data class Changes(
    val added: List<String> = emptyList(),
    val changed: List<String> = emptyList(),
    val deleted: List<String> = emptyList(),
) {

    fun isEmpty(): Boolean = added.isEmpty() && changed.isEmpty() && deleted.isEmpty()

    fun groups(): List<Pair<String, List<String>>> =
        listOf("added" to added, "changed" to changed, "deleted" to deleted)

    companion object {

        /** What the index holds that HEAD does not. */
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

                    // A rename is a name that changed, which is the only thing
                    // the list shows about a note anyway.
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

                    // The index is what a commit is made of, so that is the
                    // side the names come from.
                    formatter.scan(headTree(repo, reader), DirCacheIterator(repo.readDirCache()))
                }
            }

        private fun headTree(repo: Repository, reader: ObjectReader): AbstractTreeIterator {
            val head = repo.resolve("${Constants.HEAD}^{tree}") ?: return EmptyTreeIterator()
            return CanonicalTreeParser(null, reader, head)
        }
    }
}

/** One group of the subject line: `[a.md, b.md] added`. */
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
 * What the commit is about, read off the index.
 *
 * Every commit the app ever made said "commit from gitnote", so the history
 * recorded when something had been synced and never what. The names are the
 * paths of the notes, which is what a history is read by; a subject line that
 * would grow without end counts the rest and lists them underneath.
 */
internal fun commitMessage(changes: Changes, merging: Boolean, fallback: String): String {
    if (changes.isEmpty()) {
        // A merge that changed no file still needs a commit to close it, and
        // that is the one thing it can honestly be called.
        return if (merging) "Merge" else fallback
    }

    val subject = changes.groups()
        .mapNotNull { (verb, paths) -> subjectGroup(paths, verb) }
        .joinToString(", ")

    // Only when the subject had to leave names out: repeating three paths
    // underneath the line that already names them says nothing.
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

/** [commitMessage] for a repository, which is where the two inputs come from. */
internal fun commitMessage(repo: Repository, fallback: String): String = commitMessage(
    changes = Changes.of(repo),
    merging = repo.isMerging(),
    fallback = fallback,
)

/** Whether a merge is standing open, waiting for the commit that ends it. */
internal fun Repository.isMerging(): Boolean = repositoryState.let {
    it == RepositoryState.MERGING || it == RepositoryState.MERGING_RESOLVED
}
