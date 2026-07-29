package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.helper.sshFingerprint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SshFingerprintTest {

    /** The pair `ssh-keygen -t ed25519` produces, and what `-l` says about it. */
    private val publicKey =
        "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIJk8f3q2hZ5Q0mQ8dY6Q1nT2mR7pW9xV4kL0aB3cD5eF diffusion"

    @Test
    fun a_key_reads_as_the_fingerprint_ssh_keygen_prints() {
        assertEquals(
            "SHA256:FFNv+AKfD7RM0Wr9b80LaisrSgO7l5s+CcTcNVYnu88",
            sshFingerprint(publicKey)
        )
    }

    @Test
    fun the_comment_does_not_count() {
        assertEquals(
            sshFingerprint(publicKey),
            sshFingerprint(publicKey.substringBeforeLast(' '))
        )
    }

    @Test
    fun surrounding_whitespace_does_not_count() {
        assertEquals(sshFingerprint(publicKey), sshFingerprint("\n  $publicKey  \n"))
    }

    @Test
    fun what_is_not_a_public_key_has_no_fingerprint() {
        assertNull(sshFingerprint(""))
        assertNull(sshFingerprint("ssh-ed25519"))
        assertNull(sshFingerprint("-----BEGIN OPENSSH PRIVATE KEY-----"))
        assertNull(sshFingerprint("ssh-ed25519 not-base64!!"))
    }
}
