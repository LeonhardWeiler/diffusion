package io.github.leonhardweiler.diffusion.manager.git

import org.eclipse.jgit.lib.ProgressMonitor

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
