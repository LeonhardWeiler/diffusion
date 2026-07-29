package io.github.leonhardweiler.diffusion

import com.jcraft.jsch.HostKeyRepository
import io.github.leonhardweiler.diffusion.manager.git.PinnedHostKeys
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Trust on first use, per host **and** key type.
 *
 * A host has several host keys — github.com answers with an ed25519, an ecdsa and
 * an rsa one — and which of them is presented is whatever the two sides agree on.
 * Pinned by host alone, the fingerprint of one was compared against the
 * fingerprint of another, and every connection read as a host key that had
 * changed.
 */
class PinnedHostKeysTest {

    private lateinit var root: File
    private lateinit var file: File
    private lateinit var pins: PinnedHostKeys

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("diffusion-pins").toFile()
        file = File(root, ".ssh/pinned_hosts")
        pins = PinnedHostKeys(file)
    }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    /** A host key blob: its type, then the key, each behind its length. */
    private fun hostKey(type: String, key: String): ByteArray {
        val out = ByteArrayOutputStream()

        for (part in listOf(type.toByteArray(), key.toByteArray())) {
            out.write(part.size ushr 24)
            out.write(part.size ushr 16)
            out.write(part.size ushr 8)
            out.write(part.size)
            out.write(part)
        }

        return out.toByteArray()
    }

    private fun fingerprintOf(key: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(key)
            .joinToString("") { "%02x".format(it) }

    private val ed25519 = hostKey("ssh-ed25519", "the ed25519 host key")
    private val ecdsa = hostKey("ecdsa-sha2-nistp256", "the ecdsa host key")

    @Test
    fun aHostNobodyHasSeenIsPinned() {
        assertEquals(HostKeyRepository.OK, pins.check("github.com", ed25519))

        assertEquals(
            "github.com ssh-ed25519 ${fingerprintOf(ed25519)}",
            file.readLines().single(),
        )
    }

    @Test
    fun theSameKeyIsAcceptedAgain() {
        pins.check("github.com", ed25519)

        assertEquals(HostKeyRepository.OK, pins.check("github.com", ed25519))
        assertEquals(1, file.readLines().size, "and is not written down twice")
    }

    @Test
    fun anotherKeyOfTheSameTypeIsRefused() {
        pins.check("github.com", ed25519)

        val impostor = hostKey("ssh-ed25519", "somebody else's key")

        assertEquals(HostKeyRepository.CHANGED, pins.check("github.com", impostor))
    }

    @Test
    fun anotherTypeOfTheSameHostIsItsOwnPin() {
        pins.check("github.com", ed25519)

        assertEquals(HostKeyRepository.OK, pins.check("github.com", ecdsa))
        assertEquals(2, file.readLines().size)
    }

    @Test
    fun aHostOnAPortIsTheSameHost() {
        pins.check("github.com", ed25519)

        assertEquals(HostKeyRepository.OK, pins.check("[github.com]:22", ed25519))
        assertEquals(1, file.readLines().size)
    }

    @Test
    fun anOldPinWithoutAKeyTypeStillMatches() {
        file.parentFile.mkdirs()
        file.writeText("github.com ${fingerprintOf(ed25519)}\n")

        assertEquals(HostKeyRepository.OK, pins.check("github.com", ed25519))

        assertTrue(
            file.readLines().any { it == "github.com ssh-ed25519 ${fingerprintOf(ed25519)}" },
            "and is carried over to a line that says which key it was",
        )
    }

    /**
     * The failure that sent this app back to the setup screen: the old line was
     * written from whichever key libssh2 negotiated, and jsch negotiates another.
     * Both are github's, and neither can be told apart from a key that changed —
     * so the new one is pinned rather than refused.
     */
    @Test
    fun anOldPinOfAnotherKeyDoesNotRefuseTheHost() {
        file.parentFile.mkdirs()
        file.writeText("github.com ${fingerprintOf(ecdsa)}\n")

        assertEquals(HostKeyRepository.OK, pins.check("github.com", ed25519))

        assertTrue(
            file.readLines().any { it == "github.com ssh-ed25519 ${fingerprintOf(ed25519)}" },
        )
    }

    /** Once there is a typed line, a key that does not match it is refused. */
    @Test
    fun anOldPinDoesNotWeakenTheOnesWrittenSince() {
        file.parentFile.mkdirs()
        file.writeText("github.com ${fingerprintOf(ecdsa)}\n")
        pins.check("github.com", ed25519)

        val impostor = hostKey("ssh-ed25519", "somebody else's key")

        assertEquals(HostKeyRepository.CHANGED, pins.check("github.com", impostor))
    }

    @Test
    fun oneHostSaysNothingAboutAnother() {
        pins.check("github.com", ed25519)

        val other = hostKey("ssh-ed25519", "the gitlab host key")

        assertEquals(HostKeyRepository.OK, pins.check("gitlab.com", other))
        assertEquals(HostKeyRepository.CHANGED, pins.check("gitlab.com", ed25519))
    }

    @Test
    fun anEmptyFileIsNoPin() {
        assertEquals(HostKeyRepository.NOT_INCLUDED, pins.check("github.com", ByteArray(0)))
        assertEquals(HostKeyRepository.NOT_INCLUDED, pins.check("", ed25519))
    }
}
