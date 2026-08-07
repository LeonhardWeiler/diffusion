package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.manager.git.MergeConflictException
import io.github.leonhardweiler.diffusion.manager.git.UnresolvedConflictException
import io.github.leonhardweiler.diffusion.manager.git.commitAll
import io.github.leonhardweiler.diffusion.manager.git.currentBranch
import io.github.leonhardweiler.diffusion.manager.git.lastCommit
import io.github.leonhardweiler.diffusion.manager.git.pull
import io.github.leonhardweiler.diffusion.manager.git.push
import io.github.leonhardweiler.diffusion.manager.git.setRemoteUrl
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.RepositoryState
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GitRemoteTest : GitFixture() {
    @Test
    fun whatWasPushedIsWhatTheOtherSidePulls() {
        git.write("note.md", "one")
        commitAll(git, author, "fallback")
        push(git, null)

        val other = workingRepo("there")
        pull(other, null, author)

        assertEquals("one", other.read("note.md"))
        other.close()
    }

    @Test
    fun aPullThatOnlyBringsNewsIsAFastForward() {
        git.write("note.md", "one")
        commitAll(git, author, "fallback")
        push(git, null)

        val other = workingRepo("there")
        pull(other, null, author)
        val before = lastCommit(other.repository)

        git.write("note.md", "two")
        commitAll(git, author, "fallback")
        push(git, null)

        pull(other, null, author)

        assertEquals("two", other.read("note.md"))
        assertEquals(lastCommit(git.repository), lastCommit(other.repository))
        assertEquals(1, other.headParents(), "a fast forward is not a merge")
        assertNotNull(before)
        other.close()
    }

    @Test
    fun twoSidesThatChangedDifferentNotesAreMerged() {
        git.write("shared.md", "one")
        commitAll(git, author, "fallback")
        push(git, null)

        val other = workingRepo("there")
        pull(other, null, author)

        other.write("theirs.md", "theirs")
        commitAll(other, author, "fallback")
        push(other, null)

        git.write("mine.md", "mine")
        commitAll(git, author, "fallback")
        pull(git, null, author)

        assertEquals("theirs", git.read("theirs.md"))
        assertEquals("mine", git.read("mine.md"))
        assertEquals(2, git.headParents(), "the merge names both sides")
        assertEquals(RepositoryState.SAFE, git.repository.repositoryState)
        other.close()
    }

    @Test
    fun aRepositoryWithNothingCommittedSyncsWithoutSayingAnything() {
        assertNull(lastCommit(git.repository))

        pull(git, null, author)
        push(git, null)

        assertNull(remote.repository.resolve("${Constants.R_HEADS}main"))

        git.write("note.md", "one")
        commitAll(git, author, "fallback")
        push(git, null)

        assertEquals(lastCommit(git.repository), remote.repository.resolve("${Constants.R_HEADS}main")?.name)
    }

    @Test
    fun aBranchThatIsCalledSomethingElseIsPushedAndPulledLikeAnyOther() {
        val theirs = Git.init()
            .setDirectory(File(root, "notes-branch"))
            .setInitialBranch("notes")
            .call()
        setRemoteUrl(theirs, remote.repository.directory.absolutePath)

        File(theirs.repository.workTree, "note.md").writeText("one")
        commitAll(theirs, author, "fallback")
        push(theirs, null)

        assertEquals("notes", currentBranch(theirs.repository))
        assertNotNull(remote.repository.resolve("${Constants.R_HEADS}notes"))

        pull(git, null, author)
        assertNull(lastCommit(git.repository))

        theirs.close()
    }

    @Test
    fun aPushThatIsBehindIsRefused() {
        git.write("note.md", "one")
        commitAll(git, author, "fallback")
        push(git, null)

        val other = workingRepo("there")
        pull(other, null, author)
        other.write("note.md", "theirs")
        commitAll(other, author, "fallback")
        push(other, null)

        git.write("note.md", "mine")
        commitAll(git, author, "fallback")

        assertFailsWith<Exception> { push(git, null) }
        other.close()
    }

    @Test
    fun aConflictIsWrittenIntoTheNoteAndStopsTheSyncUntilItIsRead() {
        git.write("note.md", "one\n")
        commitAll(git, author, "fallback")
        push(git, null)

        val other = workingRepo("there")
        pull(other, null, author)
        other.write("note.md", "theirs\n")
        commitAll(other, author, "fallback")
        push(other, null)

        git.write("note.md", "mine\n")
        commitAll(git, author, "fallback")

        val conflict = assertFailsWith<MergeConflictException> { pull(git, null, author) }
        assertEquals(listOf("note.md"), conflict.paths)

        val text = git.read("note.md")
        assertContains(text, "<<<<<<<")
        assertContains(text, "mine")
        assertContains(text, "theirs")
        assertEquals(RepositoryState.MERGING, git.repository.repositoryState)

        val unresolved = assertFailsWith<UnresolvedConflictException> {
            commitAll(git, author, "fallback")
        }
        assertEquals(listOf("note.md"), unresolved.paths)

        git.write("note.md", "mine and theirs\n")
        commitAll(git, author, "fallback")

        assertEquals(RepositoryState.SAFE, git.repository.repositoryState)
        assertEquals(2, git.headParents())

        push(git, null)
        pull(git, null, author)
        assertEquals("mine and theirs\n", git.read("note.md"))
        other.close()
    }

    @Test
    fun aNoteUnderlinedWithEqualsSignsIsNotAConflict() {
        git.write("note.md", "Heading\n=======\n\ntext\n")
        commitAll(git, author, "fallback")

        assertEquals(RepositoryState.SAFE, git.repository.repositoryState)
        assertContains(git.headMessage(), "note.md")
    }
}
