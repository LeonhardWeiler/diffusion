package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.data.index.Note
import io.github.leonhardweiler.diffusion.data.index.NoteFolder
import io.github.leonhardweiler.diffusion.data.index.NoteIndex
import io.github.leonhardweiler.diffusion.data.index.foldersIn
import io.github.leonhardweiler.diffusion.data.index.notesIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * What the note list is, now that there is no database under it: a map of the
 * repository and a few filters over it.
 *
 * Everything here is what SQL used to answer — which notes belong to a folder,
 * in what order, what a folder move does to the paths under it, and what a
 * delete takes with it. The search is not here: it reads the files and asks the
 * rust side which extensions are notes, and neither exists in a unit test.
 */
class NoteIndexTest {

    /**
     * The one date every note of [indexOf] carries. Notes of the same date are
     * ordered by path, so a test that is not about dates does not have to name
     * any — and none of them is the moment the test ran, which nothing can
     * predict.
     */
    private val sameDate = 1_000_000L

    private fun indexOf(vararg paths: String): NoteIndex =
        indexOfDated(*paths.map { it to sameDate }.toTypedArray())

    private fun indexOfDated(vararg dated: Pair<String, Long>): NoteIndex {
        val index = NoteIndex()
        val paths = dated.map { it.first }

        val folders = paths
            .map { it.substringBeforeLast("/", missingDelimiterValue = "") }
            .flatMap { path ->
                // a folder and every folder above it
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

    private fun NoteIndex.namesIn(folderPath: String) =
        state.value.notesIn(folderPath).map { it.relativePath }

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
        // a clone dates every file by the commit it came from, so a whole folder
        // of them shares one minute and the order still has to be the same twice
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
        // the date of a folder is the newest note under it, however deep
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
        // and what stood beside it stayed where it was
        assertEquals(listOf("other/e.md"), index.namesIn("other"))
        // the row of the folder itself moved too, id and all
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

    /**
     * The list is a [kotlinx.coroutines.flow.StateFlow], which drops a value
     * equal to the one before it. A row that compares equal to the one it
     * replaces therefore never reaches the screen — which is what renaming
     * `testfile` to `testfile.md` used to look like: the row went on saying
     * `testfile` and answered a tap with "this note is no longer there".
     */
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
                // the id survives a rename, which is what used to hide the change
                id = row.id,
            )
        )

        assertNotEquals(before, index.state.value.notesIn(""))
    }

    /** The same for a save, which is how a new date reaches the list. */
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

    /** And for a folder, whose row carries its name and nothing else. */
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
        // a subfolder left behind is a row that opens onto nothing
        assertFalse(index.state.value.folders.containsKey("work/deep"))
        assertFalse(index.state.value.folders.containsKey("work"))

        assertTrue(index.hasNote("other/e.md"))
        assertTrue(index.state.value.folders.containsKey("other"))
    }

    @Test
    fun closing_a_repository_leaves_nothing_of_it() {
        val index = indexOf("a.md", "work/b.md")

        index.clear()

        assertEquals(emptyList(), index.namesIn(""))
        assertEquals(emptyList(), index.state.value.foldersIn(""))
    }
}
