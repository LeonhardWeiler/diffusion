package io.github.leonhardweiler.diffusion.helper

import java.security.MessageDigest
import java.util.Base64

/**
 * What one key is called on a screen offering several of them.
 *
 * The setup shows every key the app holds, and a public key is one line of
 * base64 — three of them under each other are three identical-looking buttons.
 * So a key is named by its fingerprint, in the `SHA256:` form OpenSSH writes and
 * every host shows beside a deploy key: it is the one string that can be
 * compared against what the remote says without opening anything.
 *
 * The comment goes in front of it when the key carries one, because a key that
 * was named by whoever made it is better named than by its hash. A key that does
 * not parse is shown as it stands — there is nothing truer to say about it.
 */
fun sshKeyLabel(publicKey: String): String {
    val parts = publicKey.trim().split(Regex("\\s+"), limit = 3)
    if (parts.size < 2) return publicKey.trim()

    val fingerprint = fingerprintOf(parts[1]) ?: return publicKey.trim()

    val name = parts.getOrNull(2)?.trim()?.ifEmpty { null }
        ?: parts[0].removePrefix("ssh-")

    return "$name · $fingerprint"
}

/**
 * The base64 blob hashed the way OpenSSH does it: sha256 over the raw key,
 * base64 again, and the padding dropped.
 */
private fun fingerprintOf(blob: String): String? {
    val raw = try {
        Base64.getDecoder().decode(blob)
    } catch (_: IllegalArgumentException) {
        return null
    }

    val digest = MessageDigest.getInstance("SHA-256").digest(raw)

    return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(digest)
}
