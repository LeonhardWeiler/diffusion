package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.data.index.foldersIn
import kotlin.test.Test
import kotlin.test.assertEquals

class IndexListTest : IndexFixture() {
    @Test
    fun a_folder_shows_what_stands_in_it_and_nothing_deeper() {
        val index = indexOf("a.md", "work/b.md", "work/deep/c.md")

        assertEquals(listOf("a.md"), index.namesIn(""))
        assertEquals(listOf("work/b.md"), index.namesIn("work"))
        assertEquals(listOf("work/deep/c.md"), index.namesIn("work/deep"))
    }

    @Test
    fun the_note_written_last_is_at_the_top() {
        val index = indexOfDated(
            "older.md" to 2_000L,
            "newest.md" to 9_000L,
            "oldest.md" to 1_000L,
        )

        assertEquals(
            listOf("newest.md", "older.md", "oldest.md"),
            index.namesIn("")
        )
    }

    @Test
    fun notes_of_the_same_date_are_in_path_order() {
        val index = indexOf("beta.md", "Alpha.md", "gamma.md", "delta.md")

        assertEquals(
            listOf("Alpha.md", "beta.md", "delta.md", "gamma.md"),
            index.namesIn("")
        )
    }

    @Test
    fun a_folder_stands_where_the_last_note_written_in_it_puts_it() {
        val index = indexOfDated(
            "old/a.md" to 1_000L,
            "fresh/deep/b.md" to 9_000L,
            "fresh/c.md" to 2_000L,
            "middle/d.md" to 5_000L,
        )

        assertEquals(
            listOf("fresh", "middle", "old"),
            index.state.value.foldersIn("").map { it.noteFolder.relativePath }
        )
        assertEquals(
            9_000L,
            index.state.value.foldersIn("").first().lastModifiedTimeMillis
        )
    }

    @Test
    fun a_folder_with_nothing_in_it_goes_last() {
        val index = indexOf("full/a.md")
        index.putFolder(NoteFolder.new(relativePath = "empty"))

        assertEquals(
            listOf("full", "empty"),
            index.state.value.foldersIn("").map { it.noteFolder.relativePath }
        )
    }

    @Test
    fun a_folder_row_counts_everything_under_it() {
        val index = indexOf("work/b.md", "work/deep/c.md", "work/deep/d.md", "other/e.md")

        val folders = index.state.value.foldersIn("")

        assertEquals(listOf("other", "work"), folders.map { it.noteFolder.relativePath })
        assertEquals(3, folders.first { it.noteFolder.relativePath == "work" }.noteCount)
        assertEquals(1, folders.first { it.noteFolder.relativePath == "other" }.noteCount)
    }

    @Test
    fun a_folder_beside_one_with_the_same_beginning_is_not_inside_it() {
        val index = indexOf("work/b.md", "workshop/c.md")

        assertEquals(
            1,
            index.state.value.foldersIn("").first { it.noteFolder.relativePath == "work" }.noteCount
        )
    }
}
