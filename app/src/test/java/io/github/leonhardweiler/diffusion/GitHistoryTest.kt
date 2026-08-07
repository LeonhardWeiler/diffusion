package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.manager.git.applyCommitTimestamps
import io.github.leonhardweiler.diffusion.manager.git.cloneRepository
import io.github.leonhardweiler.diffusion.manager.git.commitAll
import io.github.leonhardweiler.diffusion.manager.git.isChange
import io.github.leonhardweiler.diffusion.manager.git.lastCommit
import io.github.leonhardweiler.diffusion.manager.git.openRepository
import io.github.leonhardweiler.diffusion.manager.git.pull
import io.github.leonhardweiler.diffusion.manager.git.push
import org.eclipse.jgit.lib.PersonIdent
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHistoryTest : GitFixture() {
    @Test
    fun aNoteIsDatedByTheCommitThatWroteIt() {
        val written = commitAt("note.md", "one", secondsAgo = 60 * 60 * 24 * 30)

        git.file("note.md").setLastModified(System.currentTimeMillis())

        applyCommitTimestamps(git)

        assertEquals(written * 1000, git.file("note.md").lastModified())
    }

    @Test
    fun datingTheNotesDoesNotMakeThemLookChanged() {
        commitAt("note.md", "one", secondsAgo = 60 * 60 * 24 * 30)
        git.file("note.md").setLastModified(System.currentTimeMillis())

        applyCommitTimestamps(git)

        assertFalse(isChange(git), "the index has to be told about the new dates")
    }

    @Test
    fun aNoteTheUserIsWritingKeepsItsOwnDate() {
        val written = commitAt("note.md", "one", secondsAgo = 60 * 60 * 24 * 30)
        git.write("note.md", "one, edited")

        val typed = System.currentTimeMillis() / 1000 * 1000
        git.file("note.md").setLastModified(typed)

        applyCommitTimestamps(git)

        assertEquals(typed, git.file("note.md").lastModified())
        assertTrue(written * 1000 < typed)
    }

    @Test
    fun aPullDatesWhatItWroteAndNothingElse() {
        git.write("mine.md", "mine")
        commitAll(git, author, "fallback")
        push(git, null)
        val mineWritten = git.file("mine.md").lastModified()

        val other = workingRepo("there")
        pull(other, null, author)

        other.write("theirs.md", "theirs")
        other.add().addFilepattern(".").call()
        val old = (System.currentTimeMillis() / 1000) - 60 * 60 * 24 * 7
        val ident = PersonIdent(author.name, author.email, Instant.ofEpochSecond(old), zone)
        other.commit().setAuthor(ident).setCommitter(ident).setMessage("theirs").call()
        push(other, null)

        pull(git, null, author)

        assertEquals(old * 1000, git.file("theirs.md").lastModified(), "dated by its commit")
        assertEquals(mineWritten, git.file("mine.md").lastModified(), "not touched by the pull")
        other.close()
    }

    @Test
    fun aClonedRepositoryCarriesTheDatesOfItsCommits() {
        val written = commitAt("note.md", "one", secondsAgo = 60 * 60 * 24 * 30)
        push(git, null)

        val progress = mutableListOf<Int>()
        val clone = cloneRepository(
            File(root, "clone"),
            remote.repository.directory.absolutePath,
            cred = null,
            onProgress = { progress += it; true },
        )

        assertEquals("one", clone.read("note.md"))
        assertEquals(written * 1000, clone.file("note.md").lastModified())
        assertTrue(progress.isNotEmpty(), "the clone says how far it has got")
        assertFalse(isChange(clone))
        clone.close()
    }

    @Test
    fun openingSomethingThatIsNotARepositoryFails() {
        val folder = File(root, "notes").apply { mkdirs() }

        assertFailsWith<Exception> { openRepository(folder) }
    }

    @Test
    fun anOpenedRepositoryIsTheOneThatWasThere() {
        git.write("note.md", "one")
        commitAll(git, author, "fallback")

        val reopened = openRepository(git.repository.workTree)

        assertEquals(lastCommit(git.repository), lastCommit(reopened.repository))
        reopened.close()
    }

    private fun commitAt(path: String, text: String, secondsAgo: Long): Long {
        git.write(path, text)
        git.add().addFilepattern(".").call()

        val at = (System.currentTimeMillis() / 1000) - secondsAgo
        val ident = PersonIdent(author.name, author.email, Instant.ofEpochSecond(at), zone)

        git.commit().setAuthor(ident).setCommitter(ident).setMessage("old").call()

        return at
    }
}
