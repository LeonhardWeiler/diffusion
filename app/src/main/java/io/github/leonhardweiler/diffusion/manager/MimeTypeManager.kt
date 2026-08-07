package io.github.leonhardweiler.diffusion.manager

import io.github.leonhardweiler.diffusion.manager.ExtensionType.Markdown
import io.github.leonhardweiler.diffusion.manager.ExtensionType.Text

enum class ExtensionType {
    Text,
    Markdown;
}

private val extensions: Map<String, ExtensionType> by lazy {
    read("text").associateWith { Text } + read("markdown").associateWith { Markdown }
}

private fun read(name: String): List<String> =
    ExtensionType::class.java.getResourceAsStream("/supported_extensions/$name.txt")
        ?.bufferedReader()
        ?.useLines { lines -> lines.map { it.trim() }.filter { it.isNotEmpty() }.toList() }
        ?: error("the list of $name extensions is missing")

// no extension at all is text: nothing appends .md behind anybody's back, so a
// note called "shopping" would otherwise be a row that refuses to open
fun extensionType(extension: String): ExtensionType? =
    if (extension.isEmpty()) Text else extensions[extension]

fun isExtensionSupported(extension: String): Boolean = extensionType(extension) != null
