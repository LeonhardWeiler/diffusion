package io.github.leonhardweiler.diffusion.manager.git

import android.util.Log
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.File
import java.io.IOException
import java.time.Instant

private const val TAG = "CommitTimestamps"

/**
 * Gives every unchanged note the time of the commit that last wrote it.
 *
 * The note list reads its dates off the filesystem, which is the only place a
 * note that was never committed has one. A checkout does not honour that: it
 * stamps every file it writes with the moment it ran, so without this a clone
 * would show a repository of years of notes as all written just now.
 *
 * Files the working tree disagrees with HEAD about are left alone. Their own
 * timestamp is the true one — it is when the user typed, which is later than any
 * commit that could speak for them.
 *
 * [only] names the notes to date, null every one of them. A pull passes the ones
 * it wrote: the rest of the working tree it did not touch, and a date it did not
 * touch is not one it may move.
 */
internal fun applyCommitTimestamps(git: Git, only: Set<String>? = null) {
    val repo = git.repository
    val workTree = repo.workTree ?: return

    val dirty = dirtyPaths(git)
    val dated = mutableSetOf<String>()

    for ((path, seconds) in commitTimestamps(repo, only)) {
        if (path in dirty) continue

        // Best effort: a note whose date could not be written still reads fine,
        // it only carries the time of the checkout.
        val file = File(workTree, path)
        if (file.setLastModified(seconds * 1000L)) {
            dated += path
        } else {
            Log.d(TAG, "could not date $path")
        }
    }

    refreshIndex(repo, workTree, dated)
}

/**
 * Writes the new dates into the index as well.
 *
 * The index remembers when it last saw each file, and that is what tells git
 * whether the working tree still agrees with it — a file whose date does not
 * match is one it has to read to find out. Left alone, dating a freshly cloned
 * repository would make every note in it look changed, which is a dot on the sync
 * button and a commit of nothing at the next sync.
 */
private fun refreshIndex(repo: Repository, workTree: File, dated: Set<String>) {
    if (dated.isEmpty()) return

    val cache = repo.lockDirCache()
    var committed = false

    try {
        for (i in 0 until cache.entryCount) {
            val entry = cache.getEntry(i)
            if (entry.pathString !in dated) continue

            // Read back rather than assumed: a filesystem stores what precision
            // it stores, and the index has to hold what a later stat will find.
            entry.setLastModified(Instant.ofEpochMilli(File(workTree, entry.pathString).lastModified()))
        }

        cache.write()
        committed = cache.commit()
    } catch (e: IOException) {
        Log.w(TAG, "could not write the dates into the index", e)
    } finally {
        if (!committed) cache.unlock()
    }
}

/**
 * Dates the notes a pull brought in, and only those.
 *
 * The sync commits before it pulls, so by the time the merge is done the notes
 * written on this device agree with HEAD as well — and dating those by their
 * commit would move every one of them to the minute the sync ran. A note written
 * on Monday and synced on Friday is from Monday. What the pull itself wrote is
 * the exception: the checkout stamped it with the moment it ran, and nothing but
 * the commit behind it can say when it was written.
 */
internal fun datePulledNotes(git: Git, before: ObjectId?) {
    val repo = git.repository

    // No commit before the pull means no working tree before it either, so
    // everything standing in it now arrived with the pull.
    if (before == null) return applyCommitTimestamps(git)

    val head = repo.resolve(Constants.HEAD) ?: return

    // the pull brought nothing, so it wrote nothing
    if (head == before) return

    applyCommitTimestamps(git, changedPaths(repo, before, head))
}

/**
 * The paths the working tree does not agree with HEAD about, whether they are
 * changed, staged or not tracked at all.
 */
private fun dirtyPaths(git: Git): Set<String> {
    val status = git.status().call()

    return buildSet {
        addAll(status.added)
        addAll(status.changed)
        addAll(status.removed)
        addAll(status.missing)
        addAll(status.modified)
        addAll(status.untracked)
        addAll(status.conflicting)
    }
}

/**
 * When each note was last changed by a commit, in seconds since the epoch.
 *
 * [only] narrows that to the paths named in it, which also ends the walk over the
 * history as soon as those have been found.
 */
private fun commitTimestamps(repo: Repository, only: Set<String>?): Map<String, Long> {
    // A repository without commits has no HEAD to walk. It has no timestamps to
    // offer either, so the files keep the ones the filesystem gives them.
    val head = repo.resolve(Constants.HEAD) ?: return emptyMap()

    val timestamps = mutableMapOf<String, Long>()

    RevWalk(repo).use { walk ->
        val headCommit = walk.parseCommit(head)

        val pending = blobPaths(repo, headCommit).toMutableSet()
        if (only != null) pending.retainAll(only)
        if (pending.isEmpty()) return emptyMap()

        // One walk over the history, taking the first commit that touches a
        // path. Walking it once per file does the same work again for every file.
        walk.markStart(headCommit)
        walk.sort(RevSort.COMMIT_TIME_DESC)

        for (commit in walk) {
            if (pending.isEmpty()) break

            val parent = commit.parents.firstOrNull()
            val time = commit.commitTime.toLong()

            for (path in changedPaths(repo, parent, commit)) {
                if (pending.remove(path)) timestamps[path] = time
            }
        }
    }

    return timestamps
}

/**
 * Every blob of a commit, not only the ones the app can read itself: the list
 * shows every file in the repository with a date beside it, and a photo that was
 * committed a year ago should not read as written the minute it was cloned.
 */
private fun blobPaths(repo: Repository, commit: RevCommit): Set<String> =
    TreeWalk(repo).use { walk ->
        walk.addTree(commit.tree)
        walk.isRecursive = true

        buildSet {
            while (walk.next()) {
                if (walk.getFileMode(0) != FileMode.TREE) add(walk.pathString)
            }
        }
    }

/**
 * The files two commits disagree about, named as they stand in [to]. A commit
 * given as null is the empty tree, which is what the first commit is compared
 * against.
 */
private fun changedPaths(repo: Repository, from: ObjectId?, to: ObjectId?): Set<String> =
    DiffFormatter(DisabledOutputStream.INSTANCE).use { formatter ->
        formatter.setRepository(repo)
        formatter.setDetectRenames(false)

        formatter.scan(from, to)
            .filter { it.changeType != DiffEntry.ChangeType.DELETE }
            .map { it.newPath }
            .toSet()
    }
