package io.github.leonhardweiler.diffusion.helper

private val CLONE_SCHEMES = listOf("ssh://", "git://", "http://", "https://")

fun repoWebUrl(cloneUrl: String): String? {
    val url = cloneUrl.trim()
    if (url.isEmpty()) return null

    val scheme = CLONE_SCHEMES.firstOrNull { url.startsWith(it, ignoreCase = true) }

    val (authority, rawPath) = when {
        scheme != null -> {
            val rest = url.substring(scheme.length)
            rest.substringBefore('/') to rest.substringAfter('/', missingDelimiterValue = "")
        }

        url.contains(':') -> url.substringBefore(':') to url.substringAfter(':')

        else -> return null
    }

    val host = authority.substringAfterLast('@').substringBefore(':')

    val path = rawPath
        .trim('/')
        .removeSuffix(".git")
        .trim('/')

    if (host.isEmpty() || path.isEmpty()) return null

    return "https://$host/$path"
}
