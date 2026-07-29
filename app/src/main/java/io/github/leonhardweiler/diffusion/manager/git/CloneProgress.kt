package io.github.leonhardweiler.diffusion.manager.git

import org.eclipse.jgit.lib.ProgressMonitor

/**
 * How far a clone has got, as one number between 0 and 100.
 *
 * JGit reports a clone as a handful of tasks one after the other — counting the
 * objects, receiving them, resolving the deltas, checking the files out — and
 * says up front how many there are going to be. The setup screen has one bar, so
 * the tasks are laid end to end on it: a task that is half done on the way
 * through three is a sixth of the whole.
 *
 * [onProgress] answering false is what a cancelled clone looks like from here.
 */
internal class CloneProgress(private val onProgress: (Int) -> Boolean) : ProgressMonitor {

    private var totalTasks = 0
    private var finishedTasks = 0

    private var taskTotal = 0
    private var taskDone = 0

    private var cancelled = false

    override fun start(totalTasks: Int) {
        this.totalTasks = totalTasks
    }

    override fun beginTask(title: String?, totalWork: Int) {
        taskTotal = totalWork
        taskDone = 0
    }

    override fun update(completed: Int) {
        taskDone += completed
        report()
    }

    override fun endTask() {
        finishedTasks++
        taskTotal = 0
        taskDone = 0
        report()
    }

    override fun isCancelled(): Boolean = cancelled

    /** Timing is the caller's business, and there is nowhere here to show it. */
    override fun showDuration(enabled: Boolean) = Unit

    private fun report() {
        val inTask = if (taskTotal > 0) taskDone.toFloat() / taskTotal else 0f

        val progress = when {
            totalTasks > 0 -> (finishedTasks + inTask) / totalTasks
            else -> inTask
        }

        if (!onProgress((progress * 100).toInt().coerceIn(0, 100))) {
            cancelled = true
        }
    }
}
