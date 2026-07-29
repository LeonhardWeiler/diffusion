package io.github.leonhardweiler.diffusion.manager

import io.github.leonhardweiler.diffusion.manager.ExtensionType.Markdown
import io.github.leonhardweiler.diffusion.manager.ExtensionType.Text


enum class ExtensionType {
    Text,
    Markdown;
}

/**
 * The extensions this app treats as notes, one file per kind.
 *
 * They are read off the classpath rather than written into the source: the lists
 * are long, and a text file with one extension per line is what a list of
 * extensions wants to be. Read once and held — every row of the note list asks
 * whether its file is a note, and the answer depends on nothing but the
 * extension.
 */
private val extensions: Map<String, ExtensionType> by lazy {
    read("text").associateWith { Text } + read("markdown").associateWith { Markdown }
}

private fun read(name: String): List<String> =
    ExtensionType::class.java.getResourceAsStream("/supported_extensions/$name.txt")
        ?.bufferedReader()
        ?.useLines { lines -> lines.map { it.trim() }.filter { it.isNotEmpty() }.toList() }
        ?: error("the list of $name extensions is missing")

/**
 * What kind of note a file with this extension is, or null for one this app does
 * not show itself and hands to another.
 *
 * No extension at all is text. A note is called whatever the person writing it
 * typed, and now that nothing appends `.md` behind their back, "shopping" is a
 * name somebody chose — it would be a row that refuses to open otherwise. A
 * LICENSE or a Makefile in the repository reads as text here for the same
 * reason, which beats handing it to whatever else happens to be installed.
 */
fun extensionType(extension: String): ExtensionType? =
    if (extension.isEmpty()) Text else extensions[extension]

/** Whether a file with this extension is one the app itself can show. */
fun isExtensionSupported(extension: String): Boolean = extensionType(extension) != null
