package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.helper.PathProblem
import io.github.leonhardweiler.diffusion.helper.keepExtension
import io.github.leonhardweiler.diffusion.helper.ResolvedPath
import io.github.leonhardweiler.diffusion.helper.resolveRepoPath
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Where a typed name lands. Renaming and moving are the same act, so this is
 * the whole of what the editor's name field and the folder rename dialog mean.
 */
class RepoPathTest {

    private fun ok(parentPath: String, typed: String): String {
        val resolved = resolveRepoPath(parentPath, typed)
        return (resolved as ResolvedPath.Ok).relativePath
    }

    private fun bad(parentPath: String, typed: String): PathProblem {
        val resolved = resolveRepoPath(parentPath, typed)
        return (resolved as ResolvedPath.Bad).problem
    }

    @Test
    fun a_plain_name_stays_where_it_is() {
        assertEquals("notes.md", ok("", "notes.md"))
        assertEquals("work/notes.md", ok("work", "notes.md"))
        assertEquals("work/deep/notes.md", ok("work/deep", "notes.md"))
    }

    @Test
    fun dot_dot_goes_a_folder_up() {
        assertEquals("notes.md", ok("work", "../notes.md"))
        assertEquals("work/notes.md", ok("work/deep", "../notes.md"))
        assertEquals("notes.md", ok("work/deep", "../../notes.md"))
    }

    @Test
    fun a_folder_name_goes_into_it() {
        assertEquals("archive/notes.md", ok("", "archive/notes.md"))
        assertEquals("work/archive/notes.md", ok("work", "archive/notes.md"))
        assertEquals("archive/notes.md", ok("work", "../archive/notes.md"))
    }

    @Test
    fun a_leading_slash_counts_from_the_root() {
        assertEquals("notes.md", ok("work/deep", "/notes.md"))
        assertEquals("archive/notes.md", ok("work/deep", "/archive/notes.md"))
    }

    @Test
    fun a_single_dot_and_empty_segments_are_nothing() {
        assertEquals("work/notes.md", ok("work", "./notes.md"))
        assertEquals("work/notes.md", ok("work", ".//notes.md"))
    }

    @Test
    fun above_the_root_is_refused() {
        assertEquals(PathProblem.AboveRoot, bad("", "../notes.md"))
        assertEquals(PathProblem.AboveRoot, bad("work", "../../notes.md"))
        assertEquals(PathProblem.AboveRoot, bad("work", "/../notes.md"))
    }

    @Test
    fun nothing_typed_is_refused_as_nothing_typed() {
        assertEquals(PathProblem.Empty, bad("work", ""))
        assertEquals(PathProblem.Empty, bad("work", "   "))
    }

    @Test
    fun a_path_naming_a_folder_says_that_is_what_it_names() {
        // a trailing slash names the folder, not a thing in it
        assertEquals(PathProblem.NamesFolder, bad("work", "archive/"))
        // and this walks back to exactly where it started
        assertEquals(PathProblem.NamesFolder, bad("", "."))
        assertEquals(PathProblem.NamesFolder, bad("work", ".."))
    }

    @Test
    fun a_segment_a_file_cannot_be_called_names_the_character() {
        // which character it was, because a colon in a name reads as ordinary
        // until somebody points at it
        assertEquals(PathProblem.InvalidCharacter(':'), bad("", "no:colons.md"))
        assertEquals(PathProblem.InvalidCharacter('*'), bad("", "star*.md"))
        assertEquals(PathProblem.InvalidCharacter('?'), bad("", "arch?ive/notes.md"))
        // the first one, of several
        assertEquals(PathProblem.InvalidCharacter('*'), bad("", "star*and:colon.md"))
    }

    @Test
    fun surrounding_whitespace_does_not_count() {
        assertEquals("work/notes.md", ok("work", "  notes.md  "))
    }

    @Test
    fun a_name_without_a_dot_keeps_the_extension_it_had() {
        assertEquals("notes.md", keepExtension("notes", "md"))
        assertEquals("../archive/notes.md", keepExtension("../archive/notes", "md"))
    }

    @Test
    fun a_name_with_a_dot_says_what_the_file_is() {
        assertEquals("notes.txt", keepExtension("notes.txt", "md"))
        // the dot is the last segment's, so a folder with one changes nothing
        assertEquals("archive.old/notes.md", keepExtension("archive.old/notes", "md"))
    }

    @Test
    fun a_file_with_no_extension_gets_no_dot() {
        assertEquals("LICENSE", keepExtension("LICENSE", ""))
    }
}
