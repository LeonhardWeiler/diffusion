package io.github.leonhardweiler.gitnote

import io.github.leonhardweiler.gitnote.helper.SshKeyValidation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SshKeyValidationTest {

    private val publicKey =
        "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIJk8f3q2hZ5Q0mQ8dY6Q1nT2mR7pW9xV4kL0aB3cD5eF GitNote"

    private val privateKey = """
        -----BEGIN OPENSSH PRIVATE KEY-----
        b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
        QyNTUxOQAAACCZPH96toWeUNJkPHWOkNZ089pke6VvcVeJC9GgdwoHcA
        -----END OPENSSH PRIVATE KEY-----
    """.trimIndent()

    @Test
    fun aGeneratedPairPasses() {
        assertTrue(SshKeyValidation.isKeyPair(publicKey, privateKey))
    }

    @Test
    fun nothingIsNotAKey() {
        assertFalse(SshKeyValidation.isPublicKey(""))
        assertFalse(SshKeyValidation.isPrivateKey(""))
        assertFalse(SshKeyValidation.isPublicKey("   "))
        assertFalse(SshKeyValidation.isPrivateKey("   "))
    }

    @Test
    fun surroundingWhitespaceDoesNotMatter() {
        assertTrue(SshKeyValidation.isPublicKey("  $publicKey\n"))
        assertTrue(SshKeyValidation.isPrivateKey("\n$privateKey  "))
    }

    @Test
    fun aCommentIsOptional() {
        assertTrue(SshKeyValidation.isPublicKey(publicKey.substringBeforeLast(' ')))
    }

    @Test
    fun anUnknownAlgorithmIsRefused() {
        assertFalse(SshKeyValidation.isPublicKey("ssh-magic AAAAC3NzaC1lZDI1NTE5 comment"))
    }

    @Test
    fun anAlgorithmWithoutAKeyIsRefused() {
        assertFalse(SshKeyValidation.isPublicKey("ssh-ed25519"))
    }

    @Test
    fun theTwoHalvesAreNotInterchangeable() {
        assertFalse(SshKeyValidation.isPrivateKey(publicKey))
        assertFalse(SshKeyValidation.isPublicKey(privateKey))
    }

    @Test
    fun aPrivateKeyWithoutItsHeaderIsRefused() {
        val body = privateKey.lines().drop(1).dropLast(1).joinToString("\n")
        assertFalse(SshKeyValidation.isPrivateKey(body))
    }

    @Test
    fun otherPrivateKeyFormatsPass() {
        assertTrue(
            SshKeyValidation.isPrivateKey(
                """
                -----BEGIN RSA PRIVATE KEY-----
                MIIEowIBAAKCAQEAqwertyuiop
                -----END RSA PRIVATE KEY-----
                """.trimIndent()
            )
        )
    }
}
