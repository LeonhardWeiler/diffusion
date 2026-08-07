package io.github.leonhardweiler.diffusion.manager

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class GitExceptionType {
    RepoAlreadyInit,
    RepoNotInit,
    MergeConflict,
    UnresolvedConflict,
    NetworkUnreachable,
    Other
}

/** What every failure of [GitManager] arrives as, with a localized message. */
class GitException(
    val type: GitExceptionType,
    message: String?
) : Exception(getMessage(type, message)) {
    companion object {
        private fun getMessage(
            type: GitExceptionType,
            message: String?
        ): String? =
            message ?: if (type != GitExceptionType.Other) type.name else null
    }

    constructor(message: String) : this(GitExceptionType.Other, message)
    constructor(type: GitExceptionType) : this(type, null)
}

/**
 * Walks the causes rather than reading the sentence on top: a key that was
 * refused and a name that would not resolve both arrive as a TransportException,
 * and only one of the two is worth keeping quiet about.
 */
internal fun isNetworkFailure(e: Throwable): Boolean {
    var cause: Throwable? = e

    while (cause != null) {
        when (cause) {
            is UnknownHostException,
            is ConnectException,
            is NoRouteToHostException,
            is SocketTimeoutException,
            is SocketException,
                -> return true
        }
        cause = cause.cause.takeIf { it != cause }
    }

    return false
}

internal fun detail(e: Throwable): String =
    e.message?.takeIf { it.isNotBlank() } ?: e::class.java.simpleName
