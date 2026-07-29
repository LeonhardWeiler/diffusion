package io.github.leonhardweiler.diffusion.helper

/** The schemes a clone url can carry in front of its host. */
private val CLONE_SCHEMES = listOf("ssh://", "git://", "http://", "https://")

/**
 * The page behind a clone url, for a browser.
 *
 * The deploy key has to be pasted into the settings of that very repository, and
 * the address of it is the one thing the setup already knows — so the step that
 * asks for it can offer the way there instead of leaving the user to find the
 * repository again by hand.
 *
 * Both shapes a clone url comes in are read: `git@host:owner/repo.git`, which is
 * scp syntax and not a url at all, and `ssh://git@host:22/owner/repo.git`. What
 * comes back is always https, because that is what a browser is for; a repository
 * whose host is not reachable over http is one the button leads nowhere for, and
 * that is the host's decision rather than something this can know beforehand.
 *
 * Null for anything with no host in it — a path on the device, mostly, which has
 * no page to open.
 */
fun repoWebUrl(cloneUrl: String): String? {
    val url = cloneUrl.trim()
    if (url.isEmpty()) return null

    val scheme = CLONE_SCHEMES.firstOrNull { url.startsWith(it, ignoreCase = true) }

    val (authority, rawPath) = when {
        scheme != null -> {
            val rest = url.substring(scheme.length)
            rest.substringBefore('/') to rest.substringAfter('/', missingDelimiterValue = "")
        }

        // scp syntax: everything up to the first colon is the host, the rest is
        // the path. A colon is what makes it one — without it there is nothing
        // here but a directory name.
        url.contains(':') -> url.substringBefore(':') to url.substringAfter(':')

        else -> return null
    }

    // the user, and the port an ssh url may carry, are neither of them part of
    // the page's address
    val host = authority.substringAfterLast('@').substringBefore(':')

    val path = rawPath
        .trim('/')
        .removeSuffix(".git")
        .trim('/')

    if (host.isEmpty() || path.isEmpty()) return null

    return "https://$host/$path"
}
