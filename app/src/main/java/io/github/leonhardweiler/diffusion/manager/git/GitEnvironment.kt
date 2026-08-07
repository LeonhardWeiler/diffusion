package io.github.leonhardweiler.diffusion.manager.git

import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import java.io.File

/**
 * Runs before the first JGit call. Without it JGit goes looking for an
 * /etc/gitconfig and a git executable to ask about it, neither of which Android
 * has, so the system and jgit configs are pointed at files that do not exist.
 */
object GitEnvironment {
    @Volatile
    private var home: File? = null

    fun sshDir(): File = File(requireHome(), ".ssh")

    fun requireHome(): File =
        home ?: error("GitEnvironment.install has not run")

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

    override fun openUserConfig(parent: Config?, fs: FS): FileBasedConfig =
        FileBasedConfig(parent, File(home, ".gitconfig"), fs)

    override fun openSystemConfig(parent: Config?, fs: FS): FileBasedConfig =
        FileBasedConfig(parent, File(home, "no-system-gitconfig"), fs)

    override fun openJGitConfig(parent: Config?, fs: FS): FileBasedConfig =
        FileBasedConfig(parent, File(home, "no-jgit-config"), fs)

    @Deprecated("Deprecated in JGit", ReplaceWith("now()"))
    @Suppress("DEPRECATION")
    override fun getCurrentTime(): Long = delegate.currentTime

    @Deprecated("Deprecated in JGit", ReplaceWith("getTimeZoneAt(instant)"))
    @Suppress("DEPRECATION")
    override fun getTimezone(whenTime: Long): Int = delegate.getTimezone(whenTime)
}
