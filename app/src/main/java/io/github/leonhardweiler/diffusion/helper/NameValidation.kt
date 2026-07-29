package io.github.leonhardweiler.diffusion.helper


class NameValidation {

    companion object {

        fun removeEndingWhiteSpace(name: String): String {
            return name.trimEnd()
        }

        /**
         * Best effort
         */
        fun check(
            name: String,
        ): Boolean = name.isNotBlank() && illegalCharacter(name) == null

        /**
         * The first character a file cannot be called by, or null for a name
         * that is fine.
         *
         * The character itself, and not only the fact that there was one: "Name
         * is invalid" is a sentence somebody reads twice while looking at a name
         * that looks perfectly ordinary to them — and a colon in a date, which
         * is what usually does it, is exactly the kind of thing one does not see
         * until it is pointed at.
         */
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
