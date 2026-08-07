package io.github.leonhardweiler.diffusion.ui.viewmodel.edit

fun String.lineStartAt(index: Int): Int =
    lastIndexOf('\n', startIndex = index).let { if (it == -1) 0 else it + 1 }

fun String.lineEndAt(index: Int): Int =
    indexOf('\n', startIndex = index).let { if (it == -1) length else it }
