package io.github.leonhardweiler.diffusion.helper

import java.security.MessageDigest
import java.util.Base64

/**
 * The key as `ssh-keygen -l` would print it: `SHA256:` and the digest of the
 * key blob, base64 without padding.
 *
 * There to be recognised, not to be checked. A key that has been on the device
 * for a while is offered again rather than replaced by a fresh one, and the
 * only way to tell which one it is, is the same string the remote shows beside
 * the deploy key it holds.
 *
 * Null when the line is not a public key at all — which is decided by
 * [SshKeyValidation.isPublicKey], because a PEM header also splits into words
 * of which the second happens to be valid base64.
 */
fun sshFingerprint(publicKey: String): String? {
    if (!SshKeyValidation.isPublicKey(publicKey)) return null

    val parts = publicKey.trim().split(Regex("\\s+"))

    val blob = runCatching { Base64.getDecoder().decode(parts[1]) }.getOrNull() ?: return null

    val digest = MessageDigest.getInstance("SHA-256").digest(blob)

    return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(digest)
}
