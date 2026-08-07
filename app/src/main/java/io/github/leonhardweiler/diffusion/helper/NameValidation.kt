package io.github.leonhardweiler.diffusion.helper

class NameValidation {
    companion object {
        fun check(
            name: String,
        ): Boolean = name.isNotBlank() && illegalCharacter(name) == null

        fun illegalCharacter(name: String): Char? =
            name.firstOrNull { ILLEGAL_CHARACTERS.contains(it) }

        private val ILLEGAL_CHARACTERS = charArrayOf(
            '/',
            '\n',
            '\r',
            '\t',
            '\u0000',
            '\u000c',
            '`',
            '?',
            '*',
            '\\',
            '<',
            '>',
            '|',
            '\"',
            ':'
        )
    }
}
