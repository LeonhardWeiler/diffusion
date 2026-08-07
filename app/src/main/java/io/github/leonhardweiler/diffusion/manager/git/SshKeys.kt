package io.github.leonhardweiler.diffusion.manager.git

import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Base64

private const val KEY_COMMENT = "Diffusion"

private const val KEY_TYPE = "ssh-ed25519"

private const val PRIVATE_KEY_HEADER = "-----BEGIN OPENSSH PRIVATE KEY-----"
private const val PRIVATE_KEY_FOOTER = "-----END OPENSSH PRIVATE KEY-----"

private const val PRIVATE_KEY_LINE_LENGTH = 70

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

private fun publicKeyBlob(public: ByteArray): ByteArray = sshBytes {
    writeString(KEY_TYPE)
    writeBlock(public)
}

private fun openSshPrivateKey(public: ByteArray, private: ByteArray): String {
    val check = ByteArray(4).also { SecureRandom().nextBytes(it) }

    val secret = sshBytes {
        write(check)
        write(check)
        writeString(KEY_TYPE)
        writeBlock(public)
        writeBlock(private + public)
        writeString(KEY_COMMENT)

        // padded to the cipher block size, which is 8 even for "none", with
        // bytes counting up from one
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
