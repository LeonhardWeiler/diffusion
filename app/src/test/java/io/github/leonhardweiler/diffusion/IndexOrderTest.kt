package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.NoteIndex
import io.github.leonhardweiler.diffusion.data.index.foldersIn
import io.github.leonhardweiler.diffusion.data.index.notesIn
import io.github.leonhardweiler.diffusion.data.index.sortDatesNow
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class IndexOrderTest : IndexFixture() {
    @Test
    fun a_note_written_again_keeps_its_place_and_shows_its_new_date() {
        val index = indexOfDated(
            "a.md" to 1_000L,
            "b.md" to 2_000L,
            "c.md" to 3_000L,
        )

        val sortDates = index.state.value.sortDatesNow()

        val row = index.state.value.notes.getValue("a.md")
        index.putNote(
            Note(
                relativePath = "a.md",
                content = "",
                lastModifiedTimeMillis = 9_000L,
                id = row.id,
            )
        )

        val shown = index.state.value.notesIn("", sortDates)

        assertEquals(listOf("c.md", "b.md", "a.md"), shown.map { it.relativePath })
        assertEquals(9_000L, shown.last().lastModifiedTimeMillis)

        assertEquals(
            listOf("a.md", "c.md", "b.md"),
            index.namesIn("")
        )
    }

    @Test
    fun a_note_the_order_has_never_seen_stands_where_its_own_date_puts_it() {
        val index = indexOfDated("a.md" to 1_000L, "b.md" to 2_000L)
        val sortDates = index.state.value.sortDatesNow()

        index.putNote(Note.new(relativePath = "fresh.md", lastModifiedTimeMillis = 9_000L))

        assertEquals(
            listOf("fresh.md", "b.md", "a.md"),
            index.state.value.notesIn("", sortDates).map { it.relativePath }
        )
    }

    @Test
    fun a_folder_stays_where_it_was_when_a_note_inside_it_is_written() {
        val index = indexOfDated(
            "old/a.md" to 1_000L,
            "fresh/b.md" to 5_000L,
        )
        val sortDates = index.state.value.sortDatesNow()

        val row = index.state.value.notes.getValue("old/a.md")
        index.putNote(
            Note(
                relativePath = "old/a.md",
                content = "",
                lastModifiedTimeMillis = 9_000L,
                id = row.id,
            )
        )

        assertEquals(
            listOf("fresh", "old"),
            index.state.value.foldersIn("", sortDates).map { it.noteFolder.relativePath }
        )
        assertEquals(
            listOf("old", "fresh"),
            index.state.value.foldersIn("").map { it.noteFolder.relativePath }
        )
    }

    @Test
    fun a_renamed_note_keeps_the_place_it_stood_in() {
        val index = indexOfDated("a.md" to 1_000L, "b.md" to 2_000L)
        val sortDates = index.state.value.sortDatesNow()

        val row = index.state.value.notes.getValue("b.md")
        index.moveNote(
            oldRelativePath = "b.md",
            note = Note(
                relativePath = "renamed.md",
                content = "",
                lastModifiedTimeMillis = row.lastModifiedTimeMillis,
                id = row.id,
            )
        )

        assertEquals(
            listOf("renamed.md", "a.md"),
            index.state.value.notesIn("", sortDates).map { it.relativePath }
        )
    }

    @Test
    fun a_read_of_the_repository_is_counted_and_a_write_is_not() {
        val root = Files.createTempDirectory("note-index").toFile()

        try {
            File(root, "a.md").writeText("a")

            val index = NoteIndex()
            assertEquals(0, index.state.value.reads)

            runBlocking { index.rebuild(root.path) }
            assertEquals(1, index.state.value.reads)

            index.putNote(Note.new(relativePath = "b.md"))
            assertEquals(1, index.state.value.reads)

            runBlocking { index.rebuild(root.path) }
            assertEquals(2, index.state.value.reads)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun closing_a_repository_leaves_nothing_of_it() {
        val index = indexOf("a.md", "work/b.md")

        index.clear()

        assertEquals(emptyList(), index.namesIn(""))
        assertEquals(emptyList(), index.state.value.foldersIn(""))
    }
}
