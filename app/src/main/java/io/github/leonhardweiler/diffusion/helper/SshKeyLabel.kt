package io.github.leonhardweiler.diffusion.helper

import java.security.MessageDigest
import java.util.Base64

fun sshKeyLabel(publicKey: String): String {
    val parts = publicKey.trim().split(Regex("\\s+"), limit = 3)
    if (parts.size < 2) return publicKey.trim()

    val fingerprint = fingerprintOf(parts[1]) ?: return publicKey.trim()

    val name = parts.getOrNull(2)?.trim()?.ifEmpty { null }
        ?: parts[0].removePrefix("ssh-")

    return "$name · $fingerprint"
}

private fun fingerprintOf(blob: String): String? {
    val raw = try {
        Base64.getDecoder().decode(blob)
    } catch (_: IllegalArgumentException) {
        return null
    }

    val digest = MessageDigest.getInstance("SHA-256").digest(raw)

    return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(digest)
}
