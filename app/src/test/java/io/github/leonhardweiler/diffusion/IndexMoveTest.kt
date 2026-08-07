package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.foldersIn
import io.github.leonhardweiler.diffusion.data.index.notesIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IndexMoveTest : IndexFixture() {
    @Test
    fun moving_a_folder_takes_the_paths_under_it_with_it() {
        val index = indexOf("work/b.md", "work/deep/c.md", "other/e.md")
        val work = index.state.value.folders.getValue("work")

        index.moveFolder(work, "archive/work")

        assertEquals(
            listOf("archive/work/b.md"),
            index.namesIn("archive/work")
        )
        assertEquals(listOf("archive/work/deep/c.md"), index.namesIn("archive/work/deep"))
        assertEquals(listOf("other/e.md"), index.namesIn("other"))
        assertEquals(work.id, index.state.value.folders.getValue("archive/work").id)
    }

    @Test
    fun a_moved_note_is_the_same_note() {
        val index = indexOf("notes.md")
        val before = index.state.value.notes.getValue("notes.md")

        index.moveNote(
            oldRelativePath = "notes.md",
            note = Note(
                relativePath = "work/notes.md",
                content = "",
                lastModifiedTimeMillis = before.lastModifiedTimeMillis,
                id = before.id,
            )
        )

        assertFalse(index.hasNote("notes.md"))
        assertTrue(index.hasNote("work/notes.md"))
        assertEquals(before.id, index.state.value.notes.getValue("work/notes.md").id)
        assertEquals("notes.md", index.state.value.notes.getValue("work/notes.md").fileName)
    }

    @Test
    fun a_renamed_note_is_a_row_that_changed() {
        val index = indexOf("testfile")
        val before = index.state.value.notesIn("")
        val row = index.state.value.notes.getValue("testfile")

        index.moveNote(
            oldRelativePath = "testfile",
            note = Note(
                relativePath = "testfile.md",
                content = "",
                lastModifiedTimeMillis = row.lastModifiedTimeMillis,
                id = row.id,
            )
        )

        assertNotEquals(before, index.state.value.notesIn(""))
    }

    @Test
    fun a_note_written_again_is_a_row_that_changed() {
        val index = indexOf("notes.md")
        val before = index.state.value.notesIn("")
        val row = index.state.value.notes.getValue("notes.md")

        index.putNote(
            Note(
                relativePath = "notes.md",
                content = "",
                lastModifiedTimeMillis = row.lastModifiedTimeMillis + 5_000L,
                id = row.id,
            )
        )

        assertNotEquals(before, index.state.value.notesIn(""))
    }

    @Test
    fun a_renamed_folder_is_a_row_that_changed() {
        val index = indexOf("work/b.md")
        val before = index.state.value.foldersIn("")

        index.moveFolder(index.state.value.folders.getValue("work"), "archive")

        assertNotEquals(before, index.state.value.foldersIn(""))
    }

    @Test
    fun deleting_a_folder_takes_its_subfolders_as_well() {
        val index = indexOf("work/b.md", "work/deep/c.md", "other/e.md")
        val work = index.state.value.folders.getValue("work")

        index.removeFolders(listOf(work))

        assertFalse(index.hasNote("work/b.md"))
        assertFalse(index.hasNote("work/deep/c.md"))
        assertFalse(index.state.value.folders.containsKey("work/deep"))
        assertFalse(index.state.value.folders.containsKey("work"))

        assertTrue(index.hasNote("other/e.md"))
        assertTrue(index.state.value.folders.containsKey("other"))
    }
}
