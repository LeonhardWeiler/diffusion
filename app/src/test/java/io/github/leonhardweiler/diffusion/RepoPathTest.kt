package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.helper.PathProblem
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
    fun a_path_naming_no_file_is_refused() {
        assertEquals(PathProblem.Empty, bad("work", ""))
        assertEquals(PathProblem.Empty, bad("work", "   "))
        // a trailing slash names the folder, not a thing in it
        assertEquals(PathProblem.Empty, bad("work", "archive/"))
        // and this walks back to exactly where it started
        assertEquals(PathProblem.Empty, bad("", "."))
    }

    @Test
    fun a_segment_a_file_cannot_be_called_is_refused() {
        assertEquals(PathProblem.InvalidName, bad("", "no:colons.md"))
        assertEquals(PathProblem.InvalidName, bad("", "star*.md"))
        assertEquals(PathProblem.InvalidName, bad("", "arch?ive/notes.md"))
    }

    @Test
    fun surrounding_whitespace_does_not_count() {
        assertEquals("work/notes.md", ok("work", "  notes.md  "))
    }
}
