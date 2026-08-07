package io.github.leonhardweiler.diffusion.helper

enum class CloneUrlKind {
    Ssh,
    Http,
    Https,
}

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

    if (trimmed.first() in "/.~") return null

    val authority = trimmed.substringBefore(':', missingDelimiterValue = "")
    val path = trimmed.substringAfter(':', missingDelimiterValue = "")

    if (authority.isEmpty() || '/' in authority) return null
    if (path.isEmpty() || host(authority).isEmpty()) return null

    return CloneUrlKind.Ssh
}

private fun host(authority: String): String =
    authority.substringAfterLast('@').substringBefore(':')
