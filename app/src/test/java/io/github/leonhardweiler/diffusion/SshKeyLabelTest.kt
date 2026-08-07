package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.helper.sshKeyLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SshKeyLabelTest {

    private val blob = "AAAAC3NzaC1lZDI1NTE5AAAAIJk8f3q2hZ5Q0mQ8dY6Q1nT2mR7pW9xV4kL0aB3cD5eF"

    /** What `ssh-keygen -lf` writes for that blob, and what a host shows for it. */
    private val fingerprint = "SHA256:FFNv+AKfD7RM0Wr9b80LaisrSgO7l5s+CcTcNVYnu88"

    @Test
    fun aKeyWithNoCommentIsNamedByItsTypeAndFingerprint() {
        assertEquals("ed25519 · $fingerprint", sshKeyLabel("ssh-ed25519 $blob"))
    }

    @Test
    fun aCommentIsTheBetterName() {
        assertEquals("phone · $fingerprint", sshKeyLabel("ssh-ed25519 $blob phone"))
    }

    @Test
    fun surroundingWhitespaceDoesNotMatter() {
        assertEquals("ed25519 · $fingerprint", sshKeyLabel("  ssh-ed25519 $blob \n"))
    }

    /**
     * Two keys are two rows of the same screen, and telling them apart is the
     * whole point of a label.
     */
    @Test
    fun twoKeysAreNamedApart() {
        val other = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIAbCdEfGhIjKlMnOpQrStUvWxYz0123456789abcd"

        assertTrue(sshKeyLabel("ssh-ed25519 $blob") != sshKeyLabel(other))
    }

    @Test
    fun somethingThatIsNotAKeyIsShownAsItStands() {
        assertEquals("nonsense", sshKeyLabel("  nonsense  "))
        assertEquals("", sshKeyLabel(""))
    }
}
