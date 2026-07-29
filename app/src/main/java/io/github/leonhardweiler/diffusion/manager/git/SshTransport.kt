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

/** What a host key is called when its blob does not say. */
private const val UNKNOWN_KEY_TYPE = "unknown"

/**
 * Host keys, pinned the first time a host is seen.
 *
 * A `known_hosts` file is something a user fills on a desktop by answering a
 * question the first time they connect. There is nobody to ask here and nothing
 * that fills the file, so the first fingerprint a host presents is written down
 * and every later connection has to match it. A host key that changes stops the
 * sync instead of asking a question that cannot be answered from a note app.
 *
 * **A pin belongs to a host and a key type together**, `host type sha256hex`, one
 * per line. A host has several host keys — github.com answers with an ed25519, an
 * ecdsa and an rsa one — and which of them is presented is decided by whatever
 * the two sides agree on. Pinned by host alone, the fingerprint of one of them
 * was compared against the fingerprint of another and every connection read as a
 * host key that had changed. That is what the lines the rust side wrote look
 * like: `host sha256hex`, with nothing saying which of the keys it was.
 */
internal class PinnedHostKeys(private val file: File) : HostKeyRepository {

    override fun check(host: String?, key: ByteArray?): Int {
        val name = hostName(host) ?: return HostKeyRepository.NOT_INCLUDED
        if (key == null || key.isEmpty()) return HostKeyRepository.NOT_INCLUDED

        val type = keyType(key)
        val fingerprint = sha256Hex(key)
        val pins = read()

        pins.firstOrNull { it.host == name && it.type == type }?.let { pinned ->
            if (pinned.fingerprint == fingerprint) return HostKeyRepository.OK

            Log.e(TAG, "the $type host key of $name changed since the last connection")
            return HostKeyRepository.CHANGED
        }

        // A line without a type is one of the old ones, and it cannot say which
        // key it was taken from. Matching it is the ordinary case — the same key
        // as before — and anything else is a key type this file has never seen,
        // which is pinned rather than refused. Refusing would mean every
        // repository set up before this asking to be set up again, over a key
        // that is not the one the old line was about.
        if (pins.any { it.host == name && it.type == null && it.fingerprint == fingerprint }) {
            Log.i(TAG, "carrying an old pin of $name over to $type")
            pin(name, type, fingerprint)
            return HostKeyRepository.OK
        }

        Log.i(TAG, "pinning the $type host key of $name")
        pin(name, type, fingerprint)
        return HostKeyRepository.OK
    }

    /** Pinning happens in [check], which is the only place a key is seen. */
    override fun add(hostkey: HostKey?, ui: UserInfo?) = Unit

    override fun remove(host: String?, type: String?) = Unit

    override fun remove(host: String?, type: String?, key: ByteArray?) = Unit

    override fun getKnownHostsRepositoryID(): String = file.path

    override fun getHostKey(): Array<HostKey> = emptyArray()

    override fun getHostKey(host: String?, type: String?): Array<HostKey> = emptyArray()

    /** One line of the file: `host type fingerprint`, or the old `host fingerprint`. */
    private data class Pin(val host: String, val type: String?, val fingerprint: String)

    private fun read(): List<Pin> {
        if (!file.exists()) return emptyList()

        return runCatching {
            file.readLines().mapNotNull { line ->
                val fields = line.trim().split(' ').filter { it.isNotEmpty() }

                when (fields.size) {
                    3 -> Pin(fields[0], fields[1], fields[2])
                    2 -> Pin(fields[0], null, fields[1])
                    else -> null
                }
            }
        }.onFailure { Log.e(TAG, "could not read the pinned host keys", it) }
            .getOrDefault(emptyList())
    }

    private fun pin(host: String, type: String, fingerprint: String) {
        runCatching {
            file.parentFile?.mkdirs()
            file.appendText("$host $type $fingerprint\n")
        }.onFailure { Log.e(TAG, "could not pin the host key of $host", it) }
    }

    /**
     * jsch names a host on a port `[host]:port`, and the pins written before this
     * were named by the host alone.
     */
    private fun hostName(host: String?): String? = host
        ?.substringBefore(':')
        ?.trim('[', ']')
        ?.takeIf { it.isNotEmpty() }

    /**
     * What the key says it is: a host key blob begins with its own type, as a
     * length and then that many bytes.
     */
    private fun keyType(key: ByteArray): String {
        if (key.size < 4) return UNKNOWN_KEY_TYPE

        val length = ((key[0].toInt() and 0xff) shl 24) or
                ((key[1].toInt() and 0xff) shl 16) or
                ((key[2].toInt() and 0xff) shl 8) or
                (key[3].toInt() and 0xff)

        if (length <= 0 || length > MAX_KEY_TYPE_LENGTH || 4 + length > key.size) {
            return UNKNOWN_KEY_TYPE
        }

        val type = String(key, 4, length, Charsets.US_ASCII)

        // A type is a name like `ssh-ed25519`. Anything else would be a line of
        // this file that cannot be read back.
        return type.takeIf { it.all { char -> char.isLetterOrDigit() || char in "-.@" } }
            ?: UNKNOWN_KEY_TYPE
    }

    private fun sha256Hex(key: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(key)
            .joinToString("") { "%02x".format(it) }

    private companion object {
        /** `ecdsa-sha2-nistp521-cert-v01@openssh.com` is the long end of it. */
        const val MAX_KEY_TYPE_LENGTH = 64
    }
}
