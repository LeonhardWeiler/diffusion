package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.helper.movedUnder
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelTest {
    @Test
    fun a_note_at_the_root_has_no_parent() {
        val note = Note.new(relativePath = "notes.md")

        assertEquals("", note.parentPath)
        assertEquals("notes.md", note.fileName)
        assertEquals("notes", note.nameWithoutExtension())
    }

    @Test
    fun a_note_in_a_folder_knows_which_one() {
        val note = Note.new(relativePath = "work/deep/notes.md")

        assertEquals("work/deep", note.parentPath)
        assertEquals("notes.md", note.fileName)
    }

    @Test
    fun a_file_without_an_extension_keeps_its_whole_name() {
        assertEquals("LICENSE", Note.new(relativePath = "LICENSE").nameWithoutExtension())
        assertEquals("a", Note.new(relativePath = "a").nameWithoutExtension())
    }

    @Test
    fun a_leading_slash_is_not_a_second_name_for_the_root() {
        assertEquals("notes.md", Note.new(relativePath = "/notes.md").relativePath)
        assertEquals("work", NoteFolder.new(relativePath = "/work/").relativePath)
    }

    @Test
    fun moving_a_folder_rewrites_what_is_under_it() {
        assertEquals("c/note.md", movedUnder("a/b/note.md", "a/b", "c"))
        assertEquals("c/deep/note.md", movedUnder("a/b/deep/note.md", "a/b", "c"))
        assertEquals("c", movedUnder("a/b", "a/b", "c"))
    }

    @Test
    fun a_moved_note_derives_its_columns_again() {
        val before = Note.new(relativePath = "a/b/note.md")
        val after = Note(
            relativePath = movedUnder(before.relativePath, "a/b", "c"),
            content = before.content,
            lastModifiedTimeMillis = before.lastModifiedTimeMillis,
            id = before.id,
        )

        assertEquals("c", after.parentPath)
        assertEquals("note.md", after.fileName)
        assertEquals(before.id, after.id)
    }
}
