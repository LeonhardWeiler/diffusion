package io.github.leonhardweiler.diffusion.helper

/** The transports a clone url can name, as far as this app has to tell them apart. */
enum class CloneUrlKind {
    Ssh,
    Http,
    Https,
}

/**
 * What kind of address this is, or null when it is not a clone url at all.
 *
 * Both shapes are read: a real url with a scheme in front of it, and the scp
 * syntax `git@host:owner/repo.git`, which is not a url and is what every forge
 * offers first. Anything else — a folder on the device, an empty field, a scheme
 * this app has no transport for — is null, which is a different answer from "an
 * address over the wrong transport": the setup says something else for each.
 */
fun cloneUrlKind(url: String): CloneUrlKind? {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return null

    val scheme = trimmed.substringBefore("://", missingDelimiterValue = "")

    if (scheme.isNotEmpty()) {
        val kind = when (scheme.lowercase()) {
            "ssh" -> CloneUrlKind.Ssh
            "http" -> CloneUrlKind.Http
            "https" -> CloneUrlKind.Https
            else -> return null
        }

        val authority = trimmed.substringAfter("://").substringBefore('/')
        return kind.takeIf { host(authority).isNotEmpty() }
    }

    // A path on the device, which is the one other thing that ends up in this
    // field. It has no host and nothing to log in as.
    if (trimmed.first() in "/.~") return null

    val authority = trimmed.substringBefore(':', missingDelimiterValue = "")
    val path = trimmed.substringAfter(':', missingDelimiterValue = "")

    if (authority.isEmpty() || '/' in authority) return null
    if (path.isEmpty() || host(authority).isEmpty()) return null

    return CloneUrlKind.Ssh
}

/** The host of an authority, without the name to log in as or the port. */
private fun host(authority: String): String =
    authority.substringAfterLast('@').substringBefore(':')
