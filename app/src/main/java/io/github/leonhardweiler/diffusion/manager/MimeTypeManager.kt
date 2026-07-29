package io.github.leonhardweiler.diffusion.manager

import io.github.leonhardweiler.diffusion.manager.ExtensionType.Markdown
import io.github.leonhardweiler.diffusion.manager.ExtensionType.Text
import java.util.concurrent.ConcurrentHashMap


enum class ExtensionType {
    Text,
    Markdown;
}

/**
 * What the rust side answered for an extension, so that it is asked once.
 *
 * Every row of the list asks whether its file is a note the app can show, and
 * the answer depends on nothing but the extension — without this, each row of
 * each frame paid a JNI transition and a binary search for it. A repository
 * holds a handful of distinct extensions, so the map stays small.
 *
 * It holds the raw number rather than the enum because a map cannot hold null,
 * and "not a note" is the answer for most of what a repository contains.
 */
private val knownExtensions = ConcurrentHashMap<String, Int>()

fun extensionType(extension: String): ExtensionType? =
    extensionTypeFromNumber(
        knownExtensions.getOrPut(extension) { extensionTypeLib(extension) }
    )

/** Whether a file with this extension is one the app itself can show. */
fun isExtensionSupported(extension: String): Boolean = extensionType(extension) != null

private fun extensionTypeFromNumber(num: Int): ExtensionType? =
    when (num) {
        0 -> null
        1 -> Text
        2 -> Markdown
        else -> throw Exception("Invalid number for ExtensionType: ^$num")
    }

private external fun extensionTypeLib(extension: String): Int
