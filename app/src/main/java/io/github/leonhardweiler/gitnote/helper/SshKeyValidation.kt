package io.github.leonhardweiler.gitnote.helper

/**
 * Whether a pair of ssh keys looks like one before it is handed to libgit2.
 *
 * This says nothing about whether the key opens the repository — only the remote
 * can answer that. It catches the cases that would otherwise reach the clone as
 * an authentication failure with no hint of what went wrong: an empty field, a
 * key that was pasted without its header, or a public key given where a private
 * one belongs.
 */
object SshKeyValidation {

    private val PUBLIC_KEY_ALGORITHMS = setOf(
        "ssh-ed25519",
        "ssh-rsa",
        "ssh-dss",
        "ecdsa-sha2-nistp256",
        "ecdsa-sha2-nistp384",
        "ecdsa-sha2-nistp521",
        "sk-ssh-ed25519@openssh.com",
        "sk-ecdsa-sha2-nistp256@openssh.com",
    )

    /** One line: the algorithm, the key itself, and an optional comment. */
    fun isPublicKey(key: String): Boolean {
        val parts = key.trim().split(Regex("\\s+"), limit = 3)
        if (parts.size < 2) return false

        return parts[0] in PUBLIC_KEY_ALGORITHMS && parts[1].isNotEmpty()
    }

    /** A PEM block, whatever the format inside it is called. */
    fun isPrivateKey(key: String): Boolean {
        val lines = key.trim().lines()
        if (lines.size < 3) return false

        val first = lines.first().trim()
        val last = lines.last().trim()

        return first.startsWith("-----BEGIN ") && first.endsWith("PRIVATE KEY-----") &&
                last.startsWith("-----END ") && last.endsWith("PRIVATE KEY-----")
    }

    /** Both halves present and each of the kind it is meant to be. */
    fun isKeyPair(publicKey: String, privateKey: String): Boolean =
        isPublicKey(publicKey) && isPrivateKey(privateKey)
}
