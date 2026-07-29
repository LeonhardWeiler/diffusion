package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.data.room.ftsEscape
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What the search box types is not what FTS4 reads.
 *
 * Its MATCH syntax gives several characters a meaning nobody typing a word
 * intends, and a query that trips over one of them is a crash rather than an
 * empty result. So a plain word gets the prefix star that makes the search
 * find as you type, and anything else is quoted whole.
 */
class FtsEscapeTest {

    @Test
    fun a_plain_word_searches_by_prefix() {
        assertEquals("holiday*", ftsEscape("holiday"))
        assertEquals("two words*", ftsEscape("two words"))
    }

    @Test
    fun anything_with_a_meaning_of_its_own_is_quoted_whole() {
        assertEquals("\"a-b\" * ", ftsEscape("a-b"))
        assertEquals("\"a:b\" * ", ftsEscape("a:b"))
        assertEquals("\"(a)\" * ", ftsEscape("(a)"))
        // the words FTS4 reads as operators, not as text
        assertEquals("\"a AND b\" * ", ftsEscape("a AND b"))
    }

    @Test
    fun a_quote_is_doubled_so_it_stays_inside_the_quotes() {
        assertEquals("\"say \"\"hi\"\"\" * ", ftsEscape("say \"hi\""))
    }
}
