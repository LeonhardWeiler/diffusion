package io.github.leonhardweiler.diffusion

import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import io.github.leonhardweiler.diffusion.helper.SshKeyValidation
import io.github.leonhardweiler.diffusion.manager.git.generateSshKeys
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * A generated key is only worth anything if the ssh library can read it back: it
 * is written out here by hand, and nothing about the format is checked by the
 * compiler.
 */
class SshKeysTest {

    private val keys = generateSshKeys()
    private val publicKey = keys.first
    private val privateKey = keys.second

    @Test
    fun thePublicHalfIsOneOpenSshLine() {
        val parts = publicKey.split(' ')

        assertEquals(3, parts.size, publicKey)
        assertEquals("ssh-ed25519", parts[0])
        assertEquals("Diffusion", parts[2])

        // type and key, both length prefixed: 4 + 11 + 4 + 32
        assertEquals(51, Base64.getDecoder().decode(parts[1]).size)
    }

    @Test
    fun thePrivateHalfIsAnOpenSshContainer() {
        assertTrue(privateKey.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----\n"))
        assertTrue(privateKey.trimEnd().endsWith("-----END OPENSSH PRIVATE KEY-----"))
        assertContains(
            Base64.getDecoder().decode(privateKey.lines().drop(1).dropLast(2).joinToString(""))
                .decodeToString(),
            "openssh-key-v1",
        )
    }

    /** What the app hands jsch to log in with is what jsch has to be able to read. */
    @Test
    fun jschReadsTheKeyBack() {
        val pair = KeyPair.load(JSch(), privateKey.toByteArray(), null)

        assertEquals(KeyPair.ED25519, pair.keyType)
        assertTrue(pair.isEncrypted.not())

        // the public half jsch derives from the private one is the one that was
        // handed out to be pasted into the forge
        val derived = Base64.getEncoder().encodeToString(pair.publicKeyBlob)
        assertEquals(publicKey.split(' ')[1], derived)
    }

    @Test
    fun theKeyPassesWhatTheSetupChecksItWith() {
        assertTrue(SshKeyValidation.isPublicKey(publicKey))
        assertTrue(SshKeyValidation.isPrivateKey(privateKey))
        assertTrue(SshKeyValidation.isKeyPair(publicKey, privateKey))
    }

    @Test
    fun everyKeyIsANewOne() {
        assertNotEquals(publicKey, generateSshKeys().first)
    }
}
