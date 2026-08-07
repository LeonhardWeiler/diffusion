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

const val NETWORK_TIMEOUT_SECONDS = 7

private const val DEFAULT_SSH_USER = "git"

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
    // one JSch of our own: the base class builds one that goes looking for the
    // key files and the known_hosts of a desktop
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
        // who we log in as is read off the remote url, never stored
        if (uri.user.isNullOrEmpty()) uri.setUser(DEFAULT_SSH_USER) else uri,
        credentialsProvider,
        fs,
        tms,
    )

    override fun getJSch(hc: OpenSshConfig.Host?, fs: FS?): JSch = jsch

    override fun configure(hc: OpenSshConfig.Host?, session: Session) {
        session.setConfig("PreferredAuthentications", "publickey")

        session.setConfig("StrictHostKeyChecking", "yes")

        session.timeout = NETWORK_TIMEOUT_SECONDS * 1000
    }
}

private const val UNKNOWN_KEY_TYPE = "unknown"

/**
 * `host type sha256hex` per line. The type is part of the pin because a host
 * answers with several host keys and which one is presented is negotiated —
 * pinned by host alone, every connection reads as a key that changed. A line
 * with two fields is one of those older pins and counts as a type never seen.
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

        if (pins.any { it.host == name && it.type == null && it.fingerprint == fingerprint }) {
            Log.i(TAG, "carrying an old pin of $name over to $type")
            pin(name, type, fingerprint)
            return HostKeyRepository.OK
        }

        Log.i(TAG, "pinning the $type host key of $name")
        pin(name, type, fingerprint)
        return HostKeyRepository.OK
    }

    override fun add(hostkey: HostKey?, ui: UserInfo?) = Unit

    override fun remove(host: String?, type: String?) = Unit

    override fun remove(host: String?, type: String?, key: ByteArray?) = Unit

    override fun getKnownHostsRepositoryID(): String = file.path

    override fun getHostKey(): Array<HostKey> = emptyArray()

    override fun getHostKey(host: String?, type: String?): Array<HostKey> = emptyArray()

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

    private fun hostName(host: String?): String? = host
        ?.substringBefore(':')
        ?.trim('[', ']')
        ?.takeIf { it.isNotEmpty() }

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

        return type.takeIf { it.all { char -> char.isLetterOrDigit() || char in "-.@" } }
            ?: UNKNOWN_KEY_TYPE
    }

    private fun sha256Hex(key: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(key)
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_KEY_TYPE_LENGTH = 64
    }
}
