package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.manager.git.commitAll
import io.github.leonhardweiler.diffusion.manager.git.isChange
import io.github.leonhardweiler.diffusion.manager.git.lastCommit
import io.github.leonhardweiler.diffusion.manager.git.remoteUrl
import io.github.leonhardweiler.diffusion.manager.git.setRemoteUrl
import io.github.leonhardweiler.diffusion.manager.git.signature
import io.github.leonhardweiler.diffusion.ui.model.GitAuthor
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevWalk
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GitCommitTest : GitFixture() {
    @Test
    fun theCommitNamesTheNoteItHolds() {
        git.write("note.md", "one")
        commitAll(git, author, "fallback")

        assertEquals("[note.md] added", git.headMessage().trim())
    }

    @Test
    fun theCommitTellsAddedFromChangedFromDeleted() {
        git.write("kept.md", "one")
        git.write("gone.md", "one")
        commitAll(git, author, "fallback")

        git.write("fresh.md", "new")
        git.write("kept.md", "two")
        git.file("gone.md").delete()
        commitAll(git, author, "fallback")

        assertEquals(
            "[fresh.md] added, [kept.md] changed, [gone.md] deleted",
            git.headMessage().trim(),
        )
    }

    @Test
    fun aLongListIsCountedInTheSubjectAndWrittenOutUnderneath() {
        repeat(5) { git.write("note$it.md", "x") }
        commitAll(git, author, "fallback")

        val message = git.headMessage()

        assertEquals(
            "[note0.md, note1.md, note2.md and 2 more] added",
            message.lines().first(),
        )
        assertContains(message, "added:\n  note0.md\n  note1.md")
        assertContains(message, "  note4.md")
    }

    @Test
    fun aCommitOfNothingKeepsTheNameItWasGiven() {
        commitAll(git, author, "Sync from Diffusion")

        assertEquals("Sync from Diffusion", git.headMessage().trim())
    }

    @Test
    fun theRemoteIsReadBackFromTheRepository() {
        assertEquals(remote.repository.directory.absolutePath, remoteUrl(git.repository))

        setRemoteUrl(git, "git@github.com:owner/repo.git")

        assertEquals("git@github.com:owner/repo.git", remoteUrl(git.repository))
    }

    @Test
    fun aRepositoryWithoutCommitsHasNoLastCommitAndNoSignature() {
        assertNull(lastCommit(git.repository))
        assertNull(signature(git.repository))
    }

    @Test
    fun theSignatureFallsBackToWhoeverWroteTheLastCommit() {
        git.write("note.md", "one")
        commitAll(git, author, "fallback")

        assertNotNull(lastCommit(git.repository))
        assertEquals(author.name to author.email, signature(git.repository))
    }

    @Test
    fun aCommitIsNeverByNobody() {
        git.write("note.md", "one")
        commitAll(git, GitAuthor(name = "", email = "  "), "fallback")

        val ident = RevWalk(git.repository).use { walk ->
            walk.parseCommit(git.repository.resolve(Constants.HEAD)).authorIdent
        }

        assertEquals(GitAuthor.DEFAULT_NAME, ident.name)
        assertEquals(GitAuthor.DEFAULT_EMAIL, ident.emailAddress)
    }

    @Test
    fun aWrittenNoteIsAChangeUntilItIsCommitted() {
        assertFalse(isChange(git))

        git.write("note.md", "one")
        assertTrue(isChange(git))

        commitAll(git, author, "fallback")
        assertFalse(isChange(git))
    }
}
