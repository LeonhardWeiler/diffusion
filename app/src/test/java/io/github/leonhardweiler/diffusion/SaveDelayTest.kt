package io.github.leonhardweiler.diffusion

import io.github.leonhardweiler.diffusion.ui.viewmodel.edit.saveDelayMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveDelayTest {
    @Test
    fun an_ordinary_note_is_written_after_the_usual_pause() {
        assertEquals(500L, saveDelayMillis(0))
        assertEquals(500L, saveDelayMillis(1_000))
        assertEquals(500L, saveDelayMillis(16 * 1024))
    }

    @Test
    fun the_pause_grows_with_the_note() {
        val small = saveDelayMillis(16 * 1024)
        val bigger = saveDelayMillis(32 * 1024)

        assertTrue(bigger > small)
        assertEquals(1_000L, bigger)
    }

    @Test
    fun and_stops_growing() {
        assertEquals(3_000L, saveDelayMillis(200 * 1024))
        assertEquals(3_000L, saveDelayMillis(Int.MAX_VALUE))
    }
}
