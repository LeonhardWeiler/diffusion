package io.github.leonhardweiler.diffusion.manager.git

import android.util.Log
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import io.github.leonhardweiler.diffusion.ui.model.Cred
import org.eclipse.jgit.api.TransportConfigCallback
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS
import java.io.File
import java.security.MessageDigest

private const val TAG = "SshTransport"

/**
 * How long a remote that says nothing is waited for, in seconds.
 *
 * The app syncs when it is opened and when it is left, which is exactly when a
 * phone is coming back from sleep — a socket that hangs there hangs in front of
 * the note list.
 */
const val NETWORK_TIMEOUT_SECONDS = 7

/**
 * The name to log in as is the one standing in the url.
 *
 * `ssh://tom@host/notes.git` authenticates as `tom`. Only a url that carries no
 * name at all falls back to this, which is the name every hosted forge answers
 * to.
 */
private const val DEFAULT_SSH_USER = "git"

/**
 * What every connection to the remote is configured with: the key to log in
 * with, the host keys to accept, and how long to wait.
 */
class SshTransportConfig(cred: Cred?) : TransportConfigCallback {

    private val sessionFactory = PinningSessionFactory(cred as? Cred.Ssh)

    override fun configure(transport: Transport) {
        transport.timeout = NETWORK_TIMEOUT_SECONDS
        if (transport is SshTransport) {
            transport.sshSessionFactory = sessionFactory
        }
    }
}

private class PinningSessionFactory(private val cred: Cred.Ssh?) : JschConfigSessionFactory() {

    /**
     * One JSch for every connection this object makes, holding the one key the
     * app was set up with. Built here rather than by the base class, which
     * would go looking for the key files and the `known_hosts` of a desktop.
     */
    private val jsch: JSch by lazy {
        JSch().apply {
            hostKeyRepository = PinnedHostKeys(File(GitEnvironment.sshDir(), "pinned_hosts"))

            cred?.let {
                addIdentity(
                    "diffusion",
                    it.privateKey.toByteArray(),
                    it.publicKey.toByteArray(),
                    it.passphrase?.toByteArray(),
                )
            }
        }
    }

    override fun getSession(
        uri: URIish,
        credentialsProvider: CredentialsProvider?,
        fs: FS?,
        tms: Int,
    ): RemoteSession = super.getSession(
        if (uri.user.isNullOrEmpty()) uri.setUser(DEFAULT_SSH_USER) else uri,
        credentialsProvider,
        fs,
        tms,
    )

    override fun getJSch(hc: OpenSshConfig.Host?, fs: FS?): JSch = jsch

    override fun configure(hc: OpenSshConfig.Host?, session: Session) {
        // The key is the only thing the app has: without this, a remote that
        // refuses it goes on to ask for a password, and there is nobody to type
        // one — the connection would hang until it timed out.
        session.setConfig("PreferredAuthentications", "publickey")

        // Refuse a host whose key is not the one that was pinned. Nothing ever
        // returns "unknown" from the repository below, which is what this would
        // otherwise stop.
        session.setConfig("StrictHostKeyChecking", "yes")

        session.timeout = NETWORK_TIMEOUT_SECONDS * 1000
    }
}

/**
 * Host keys, pinned the first time a host is seen.
 *
 * A `known_hosts` file is something a user fills on a desktop by answering a
 * question the first time they connect. There is nobody to ask here and nothing
 * that fills the file, so the first fingerprint a host presents is written down
 * and every later connection has to match it. A host key that changes stops the
 * sync instead of asking a question that cannot be answered from a note app.
 *
 * The file is the one the rust side wrote — `host sha256hex`, one per line — so
 * a repository set up before this keeps its pins.
 */
private class PinnedHostKeys(private val file: File) : HostKeyRepository {

    override fun check(host: String?, key: ByteArray?): Int {
        val name = hostName(host) ?: return HostKeyRepository.NOT_INCLUDED
        if (key == null) return HostKeyRepository.NOT_INCLUDED

        val fingerprint = sha256Hex(key)

        return when (pinned(name)) {
            null -> {
                Log.i(TAG, "pinning host key of $name")
                pin(name, fingerprint)
                HostKeyRepository.OK
            }

            fingerprint -> HostKeyRepository.OK

            else -> {
                Log.e(TAG, "the host key of $name changed since the last connection")
                HostKeyRepository.CHANGED
            }
        }
    }

    /** Pinning happens in [check], which is the only place a key is seen. */
    override fun add(hostkey: HostKey?, ui: UserInfo?) = Unit

    override fun remove(host: String?, type: String?) = Unit

    override fun remove(host: String?, type: String?, key: ByteArray?) = Unit

    override fun getKnownHostsRepositoryID(): String = file.path

    override fun getHostKey(): Array<HostKey> = emptyArray()

    override fun getHostKey(host: String?, type: String?): Array<HostKey> = emptyArray()

    private fun pinned(host: String): String? =
        file.takeIf { it.exists() }
            ?.readLines()
            ?.firstNotNullOfOrNull { line ->
                val (pinnedHost, fingerprint) = line.split(' ', limit = 2)
                    .takeIf { it.size == 2 } ?: return@firstNotNullOfOrNull null
                fingerprint.takeIf { pinnedHost == host }
            }

    private fun pin(host: String, fingerprint: String) {
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText("$host $fingerprint\n")
        }.onFailure { Log.e(TAG, "could not pin the host key of $host", it) }
    }

    /**
     * jsch names a host on a port `[host]:port`, and the pins written before
     * this were named by the host alone.
     */
    private fun hostName(host: String?): String? = host
        ?.substringBefore(':')
        ?.trim('[', ']')
        ?.takeIf { it.isNotEmpty() }

    private fun sha256Hex(key: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(key)
            .joinToString("") { "%02x".format(it) }
}
