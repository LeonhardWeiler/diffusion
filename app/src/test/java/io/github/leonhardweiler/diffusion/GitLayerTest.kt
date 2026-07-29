package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.manager.git.GitEnvironment
import io.github.leonhardweiler.diffusion.manager.git.MergeConflictException
import io.github.leonhardweiler.diffusion.manager.git.UnresolvedConflictException
import io.github.leonhardweiler.diffusion.manager.git.applyCommitTimestamps
import io.github.leonhardweiler.diffusion.manager.git.cloneRepository
import io.github.leonhardweiler.diffusion.manager.git.commitAll
import io.github.leonhardweiler.diffusion.manager.git.isChange
import io.github.leonhardweiler.diffusion.manager.git.lastCommit
import io.github.leonhardweiler.diffusion.manager.git.openRepository
import io.github.leonhardweiler.diffusion.manager.git.pull
import io.github.leonhardweiler.diffusion.manager.git.push
import io.github.leonhardweiler.diffusion.manager.git.remoteUrl
import io.github.leonhardweiler.diffusion.manager.git.setRemoteUrl
import io.github.leonhardweiler.diffusion.manager.git.signature
import io.github.leonhardweiler.diffusion.ui.model.GitAuthor
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import java.nio.file.Files
import java.util.Date
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The git layer against real repositories.
 *
 * Everything but the ssh transport is exercised here: the remote is a bare
 * repository beside the two working ones, which is a path rather than a host and
 * needs no key. What ssh does with that path is the one part a device has to say
 * whether it works.
 */
class GitLayerTest {

    private val author = GitAuthor(name = "Tester", email = "tester@example.com")

    private lateinit var root: File
    private lateinit var remote: Git
    private lateinit var git: Git

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("diffusion-git").toFile()

        // Also what keeps the developer's own ~/.gitconfig out of these
        // repositories: it is where the app points git at its own directory.
        GitEnvironment.install(File(root, "home"))

        remote = Git.init()
            .setBare(true)
            .setDirectory(File(root, "remote.git"))
            .setInitialBranch("main")
            .call()

        git = workingRepo("here")
    }

    @AfterTest
    fun tearDown() {
        git.close()
        remote.close()
        root.deleteRecursively()
    }

    private fun workingRepo(name: String): Git {
        val repo = Git.init()
            .setDirectory(File(root, name))
            .setInitialBranch("main")
            .call()

        setRemoteUrl(repo, remote.repository.directory.absolutePath)
        return repo
    }

    private fun Git.write(path: String, text: String) {
        val file = File(repository.workTree, path)
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    private fun Git.read(path: String): String = File(repository.workTree, path).readText()

    private fun Git.file(path: String): File = File(repository.workTree, path)

    private fun Git.headMessage(): String = RevWalk(repository).use { walk ->
        walk.parseCommit(repository.resolve(Constants.HEAD)).fullMessage
    }

    private fun Git.headParents(): Int = RevWalk(repository).use { walk ->
        walk.parseCommit(repository.resolve(Constants.HEAD)).parentCount
    }

    // -- what a commit is called ------------------------------------------------

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

    // -- what the repository says about itself -----------------------------------

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
    fun aWrittenNoteIsAChangeUntilItIsCommitted() {
        assertFalse(isChange(git))

        git.write("note.md", "one")
        assertTrue(isChange(git))

        commitAll(git, author, "fallback")
        assertFalse(isChange(git))
    }

    // -- sync --------------------------------------------------------------------

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

    // -- conflicts ----------------------------------------------------------------

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

        // both versions are in the note now, which is what makes it something
        // that can be read and fixed
        val text = git.read("note.md")
        assertContains(text, "<<<<<<<")
        assertContains(text, "mine")
        assertContains(text, "theirs")
        assertEquals(RepositoryState.MERGING, git.repository.repositoryState)

        // committing that would write the markers into the history
        val unresolved = assertFailsWith<UnresolvedConflictException> {
            commitAll(git, author, "fallback")
        }
        assertEquals(listOf("note.md"), unresolved.paths)

        // edited down, the same commit ends the merge — and it names both sides,
        // or the next pull would fetch the same conflict again
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

    // -- dates ---------------------------------------------------------------------

    @Test
    fun aNoteIsDatedByTheCommitThatWroteIt() {
        val written = commitAt("note.md", "one", secondsAgo = 60 * 60 * 24 * 30)

        // what a checkout would have left behind
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
        // a note written here, whose own date is the truth about it: it was
        // typed on this device, and the sync it went out with says nothing
        // about when
        git.write("mine.md", "mine")
        commitAll(git, author, "fallback")
        push(git, null)
        val mineWritten = git.file("mine.md").lastModified()

        // and one written on the other device a week ago
        val other = workingRepo("there")
        pull(other, null, author)

        other.write("theirs.md", "theirs")
        other.add().addFilepattern(".").call()
        val old = (System.currentTimeMillis() / 1000) - 60 * 60 * 24 * 7
        val ident = PersonIdent(author.name, author.email, Date(old * 1000), TimeZone.getDefault())
        other.commit().setAuthor(ident).setCommitter(ident).setMessage("theirs").call()
        push(other, null)

        pull(git, null, author)

        assertEquals(old * 1000, git.file("theirs.md").lastModified(), "dated by its commit")
        assertEquals(mineWritten, git.file("mine.md").lastModified(), "not touched by the pull")
        other.close()
    }

    // -- opening and cloning ---------------------------------------------------------

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

    /** A note committed [secondsAgo] ago, as one written on another day would be. */
    private fun commitAt(path: String, text: String, secondsAgo: Long): Long {
        git.write(path, text)
        git.add().addFilepattern(".").call()

        val at = (System.currentTimeMillis() / 1000) - secondsAgo
        val ident = PersonIdent(author.name, author.email, Date(at * 1000), TimeZone.getDefault())

        git.commit().setAuthor(ident).setCommitter(ident).setMessage("old").call()

        return at
    }
}
