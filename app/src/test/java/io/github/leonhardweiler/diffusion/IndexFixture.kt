package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.data.index.NoteIndex
import io.github.leonhardweiler.diffusion.data.index.notesIn

/** An index built by hand: the paths, their folders, and a date each. */
abstract class IndexFixture {
    protected val sameDate = 1_000_000L

    protected fun indexOf(vararg paths: String): NoteIndex =
        indexOfDated(*paths.map { it to sameDate }.toTypedArray())

    protected fun indexOfDated(vararg dated: Pair<String, Long>): NoteIndex {
        val index = NoteIndex()
        val paths = dated.map { it.first }

        val folders = paths
            .map { it.substringBeforeLast("/", missingDelimiterValue = "") }
            .flatMap { path ->
                generateSequence(path) { it.substringBeforeLast("/", missingDelimiterValue = "") }
                    .takeWhile { it.isNotEmpty() }
                    .toList()
            }
            .distinct()

        index.putFolder(NoteFolder.new(relativePath = ""))
        folders.forEach { index.putFolder(NoteFolder.new(relativePath = it)) }
        dated.forEach { (path, date) ->
            index.putNote(Note.new(relativePath = path, lastModifiedTimeMillis = date))
        }

        return index
    }

    protected fun NoteIndex.namesIn(folderPath: String) =
        state.value.notesIn(folderPath).map { it.relativePath }
}
