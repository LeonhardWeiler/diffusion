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

fun extensionType(extension: String): ExtensionType? = extensions[extension]

/** Whether a file with this extension is one the app itself can show. */
fun isExtensionSupported(extension: String): Boolean = extensionType(extension) != null
