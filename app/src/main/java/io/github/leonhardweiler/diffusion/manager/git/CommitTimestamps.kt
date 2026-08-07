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

internal fun applyCommitTimestamps(git: Git, only: Set<String>? = null) {
    val repo = git.repository
    val workTree = repo.workTree ?: return

    val dirty = dirtyPaths(git)
    val dated = mutableSetOf<String>()

    for ((path, seconds) in commitTimestamps(repo, only)) {
        if (path in dirty) continue

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
 * The index remembers when it last saw each file, and that is what decides
 * whether the working tree still agrees with it. Without this a fresh clone
 * reads as changed all over: a dot on the sync button and a commit of nothing.
 */
private fun refreshIndex(repo: Repository, workTree: File, dated: Set<String>) {
    if (dated.isEmpty()) return

    val cache = repo.lockDirCache()
    var committed = false

    try {
        for (i in 0 until cache.entryCount) {
            val entry = cache.getEntry(i)
            if (entry.pathString !in dated) continue

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

internal fun datePulledNotes(git: Git, before: ObjectId?) {
    val repo = git.repository

    if (before == null) return applyCommitTimestamps(git)

    val head = repo.resolve(Constants.HEAD) ?: return

    if (head == before) return

    applyCommitTimestamps(git, changedPaths(repo, before, head))
}

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

private fun commitTimestamps(repo: Repository, only: Set<String>?): Map<String, Long> {
    val head = repo.resolve(Constants.HEAD) ?: return emptyMap()

    val timestamps = mutableMapOf<String, Long>()

    RevWalk(repo).use { walk ->
        val headCommit = walk.parseCommit(head)

        val pending = blobPaths(repo, headCommit).toMutableSet()
        if (only != null) pending.retainAll(only)
        if (pending.isEmpty()) return emptyMap()

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

private fun changedPaths(repo: Repository, from: ObjectId?, to: ObjectId?): Set<String> =
    DiffFormatter(DisabledOutputStream.INSTANCE).use { formatter ->
        formatter.setRepository(repo)
        formatter.setDetectRenames(false)

        formatter.scan(from, to)
            .filter { it.changeType != DiffEntry.ChangeType.DELETE }
            .map { it.newPath }
            .toSet()
    }
