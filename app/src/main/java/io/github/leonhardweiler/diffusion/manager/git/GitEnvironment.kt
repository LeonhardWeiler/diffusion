package io.github.leonhardweiler.diffusion.manager.git

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import java.io.File

/**
 * The machine JGit thinks it is running on.
 *
 * JGit expects the surroundings of a desktop: a home directory with a
 * `.gitconfig` in it, an `/etc/gitconfig` beside the git it can ask for its
 * system settings, an `~/.ssh` to read keys and hosts from. Android has none of
 * that — there is no `git` to run and no home to speak of, and left alone JGit
 * spends a process launch per repository finding that out.
 *
 * So it is told where home is (the app's own private directory, which is also
 * where the pinned host keys live) and handed empty system settings. What is
 * left is the user config, which the app never writes either but which a
 * repository moved here from a desktop could carry.
 */
object GitEnvironment {

    @Volatile
    private var home: File? = null

    /** Where the ssh files of this app live: `filesDir/.ssh`. */
    fun sshDir(): File = File(requireHome(), ".ssh")

    fun requireHome(): File =
        home ?: error("GitEnvironment.install has not run")

    /**
     * Points JGit at [homeDir]. Runs once — a second call would only replace the
     * reader with an equal one, and JGit caches what it read off the first.
     */
    @Synchronized
    fun install(homeDir: File) {
        if (home != null) return
        home = homeDir

        homeDir.mkdirs()
        File(homeDir, ".ssh").mkdirs()

        SystemReader.setInstance(AppSystemReader(homeDir, SystemReader.getInstance()))
    }
}

private class AppSystemReader(
    private val home: File,
    private val delegate: SystemReader,
) : SystemReader() {

    override fun getHostname(): String = delegate.hostname

    override fun getenv(variable: String?): String? = when (variable) {
        "HOME" -> home.path
        else -> delegate.getenv(variable)
    }

    override fun getProperty(key: String?): String? = when (key) {
        "user.home" -> home.path
        else -> delegate.getProperty(key)
    }

    /** A `.gitconfig` in the app's directory, which is the only one reachable. */
    override fun openUserConfig(parent: Config?, fs: FS): FileBasedConfig =
        FileBasedConfig(parent, File(home, ".gitconfig"), fs)

    /**
     * Nothing. The file behind it is deliberately one that does not exist: a
     * [FileBasedConfig] that finds nothing loads as empty, and asking JGit for
     * the system config the ordinary way makes it go looking for a `git`
     * executable to ask.
     */
    override fun openSystemConfig(parent: Config?, fs: FS): FileBasedConfig =
        FileBasedConfig(parent, File(home, "no-system-gitconfig"), fs)

    override fun openJGitConfig(parent: Config?, fs: FS): FileBasedConfig =
        FileBasedConfig(parent, File(home, "no-jgit-config"), fs)

    // Both are deprecated in JGit and both are still abstract, so there is
    // nothing to do but hand them on to the reader that was there before.
    @Deprecated("Deprecated in JGit", ReplaceWith("now()"))
    @Suppress("DEPRECATION")
    override fun getCurrentTime(): Long = delegate.currentTime

    @Deprecated("Deprecated in JGit", ReplaceWith("getTimeZoneAt(instant)"))
    @Suppress("DEPRECATION")
    override fun getTimezone(whenTime: Long): Int = delegate.getTimezone(whenTime)
}
