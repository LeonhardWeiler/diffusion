package io.github.leonhardweiler.diffusion.manager.git

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Base64

/** What a key made here is called, in the comment both halves carry. */
private const val KEY_COMMENT = "Diffusion"

private const val KEY_TYPE = "ssh-ed25519"

/** The header and footer OpenSSH wraps a private key in. */
private const val PRIVATE_KEY_HEADER = "-----BEGIN OPENSSH PRIVATE KEY-----"
private const val PRIVATE_KEY_FOOTER = "-----END OPENSSH PRIVATE KEY-----"

/** What `ssh-keygen` wraps the base64 of a private key at. */
private const val PRIVATE_KEY_LINE_LENGTH = 70

/**
 * A new ed25519 pair, as OpenSSH writes them: the public half is one line, the
 * private half is the `openssh-key-v1` container, unencrypted.
 *
 * ed25519 because it is short enough to be copied by hand off a phone screen,
 * and because every forge that takes a deploy key takes one. Written out here
 * rather than handed to a library: the format is a length-prefixed blob and a
 * base64 wrapper, and the alternative was a second crypto dependency for it.
 *
 * @return the public key first, the private key second.
 */
fun generateSshKeys(): Pair<String, String> {
    val generator = Ed25519KeyPairGenerator()
    generator.init(Ed25519KeyGenerationParameters(SecureRandom()))

    val pair = generator.generateKeyPair()
    val public = (pair.public as Ed25519PublicKeyParameters).encoded
    val private = (pair.private as Ed25519PrivateKeyParameters).encoded

    return openSshPublicKey(public) to openSshPrivateKey(public, private)
}

private fun openSshPublicKey(public: ByteArray): String {
    val blob = Base64.getEncoder().encodeToString(publicKeyBlob(public))
    return "$KEY_TYPE $blob $KEY_COMMENT"
}

/** `string(type) string(key)`, which is what a public key is on the wire. */
private fun publicKeyBlob(public: ByteArray): ByteArray = sshBytes {
    writeString(KEY_TYPE)
    writeBlock(public)
}

private fun openSshPrivateKey(public: ByteArray, private: ByteArray): String {
    // The two halves of the check are compared after decryption to tell a wrong
    // passphrase from a right one. Nothing here is encrypted, but the reader
    // still looks at them.
    val check = ByteArray(4).also { SecureRandom().nextBytes(it) }

    val secret = sshBytes {
        write(check)
        write(check)
        writeString(KEY_TYPE)
        writeBlock(public)
        // OpenSSH keeps the public half inside the private one as well
        writeBlock(private + public)
        writeString(KEY_COMMENT)

        // Padded to the block size of the cipher, which is 8 even for "none",
        // with bytes counting up from one.
        var pad = 1
        while (size() % 8 != 0) write(pad++)
    }

    val container = sshBytes {
        write("openssh-key-v1".toByteArray(Charsets.US_ASCII))
        write(0)
        writeString("none") // cipher
        writeString("none") // key derivation
        writeBlock(ByteArray(0)) // its options
        writeInt(1) // one key follows
        writeBlock(publicKeyBlob(public))
        writeBlock(secret)
    }

    val body = Base64.getEncoder()
        .encodeToString(container)
        .chunked(PRIVATE_KEY_LINE_LENGTH)
        .joinToString("\n")

    return "$PRIVATE_KEY_HEADER\n$body\n$PRIVATE_KEY_FOOTER\n"
}

/**
 * The one shape everything in an ssh key file has: a four byte length in front
 * of the bytes it counts.
 */
private class SshWriter : ByteArrayOutputStream() {

    fun writeInt(value: Int) {
        write(value ushr 24)
        write(value ushr 16)
        write(value ushr 8)
        write(value)
    }

    fun writeBlock(bytes: ByteArray) {
        writeInt(bytes.size)
        write(bytes)
    }

    fun writeString(value: String) = writeBlock(value.toByteArray(Charsets.UTF_8))
}

private fun sshBytes(block: SshWriter.() -> Unit): ByteArray =
    SshWriter().apply(block).toByteArray()
