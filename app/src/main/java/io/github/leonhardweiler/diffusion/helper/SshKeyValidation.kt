package io.github.leonhardweiler.diffusion.helper

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

    fun isPublicKey(key: String): Boolean {
        val parts = key.trim().split(Regex("\\s+"), limit = 3)
        if (parts.size < 2) return false

        return parts[0] in PUBLIC_KEY_ALGORITHMS && parts[1].isNotEmpty()
    }

    fun isPrivateKey(key: String): Boolean {
        val lines = key.trim().lines()
        if (lines.size < 3) return false

        val first = lines.first().trim()
        val last = lines.last().trim()

        return first.startsWith("-----BEGIN ") && first.endsWith("PRIVATE KEY-----") &&
                last.startsWith("-----END ") && last.endsWith("PRIVATE KEY-----")
    }

    fun isKeyPair(publicKey: String, privateKey: String): Boolean =
        isPublicKey(publicKey) && isPrivateKey(privateKey)
}
