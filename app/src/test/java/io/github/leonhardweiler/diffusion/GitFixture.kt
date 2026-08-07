package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.manager.git.GitEnvironment
import io.github.leonhardweiler.diffusion.manager.git.setRemoteUrl
import io.github.leonhardweiler.diffusion.ui.model.GitAuthor
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import java.nio.file.Files
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * A bare repository standing in for the remote and a working one beside it, in
 * a temp directory. Every function in manager/git takes the Git it works on, so
 * the whole layer can be run against real repositories.
 */
abstract class GitFixture {
    protected val author = GitAuthor(name = "Tester", email = "tester@example.com")
    protected val zone: ZoneId = ZoneId.systemDefault()

    protected lateinit var root: File
    protected lateinit var remote: Git
    protected lateinit var git: Git

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("diffusion-git").toFile()

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

    protected fun workingRepo(name: String): Git {
        val repo = Git.init()
            .setDirectory(File(root, name))
            .setInitialBranch("main")
            .call()

        setRemoteUrl(repo, remote.repository.directory.absolutePath)
        return repo
    }

    protected fun Git.write(path: String, text: String) {
        val file = File(repository.workTree, path)
        file.parentFile?.mkdirs()
        file.writeText(text)
    }

    protected fun Git.read(path: String): String = File(repository.workTree, path).readText()

    protected fun Git.file(path: String): File = File(repository.workTree, path)

    protected fun Git.headMessage(): String = RevWalk(repository).use { walk ->
        walk.parseCommit(repository.resolve(Constants.HEAD)).fullMessage
    }

    protected fun Git.headParents(): Int = RevWalk(repository).use { walk ->
        walk.parseCommit(repository.resolve(Constants.HEAD)).parentCount
    }
}
