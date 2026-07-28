package io.github.leonhardweiler.gitnote.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The app as it is installed for the recording, without any suffix. */
private const val PACKAGE = "io.github.leonhardweiler.gitnote"

/** How long to wait for a screen to have finished drawing itself. */
private const val IDLE_MS = 2_000L

/**
 * Records what the app runs through when it is used, so that the release does
 * not interpret its way through it once per install.
 *
 * ```
 * ./gradlew :app:generateBaselineProfile
 * ```
 *
 * Needs a device or an emulator that is attached, and one where the app has
 * already been set up: the journey below is the note list, a note, and the way
 * back out — a repository that has not been chosen yet leads to the setup
 * instead, and a profile recorded from that says nothing about the app in use.
 * The result is written to app/src/release/generated/baselineProfiles.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE,
        // the first frames after tapping the icon are the ones a cold start is
        // judged by, and they are the same frames every time
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        device.waitForIdle(IDLE_MS)
        device.scrollTheList()

        // Into the first note and back out, which is the whole of what the app
        // is for and the most expensive thing it composes.
        device.openTheFirstNote()
        device.waitForIdle(IDLE_MS)
        device.pressBack()
        device.waitForIdle(IDLE_MS)
    }

    private fun UiDevice.scrollTheList() {
        val list = findObject(androidx.test.uiautomator.By.scrollable(true)) ?: return

        list.setGestureMargin(displayWidth / 5)
        repeat(2) {
            list.fling(Direction.DOWN)
            waitForIdle(IDLE_MS)
        }
        list.fling(Direction.UP)
        waitForIdle(IDLE_MS)
    }

    /**
     * Taps where the first row is. By position rather than by name: the rows
     * carry the names of whatever notes the device happens to hold.
     */
    private fun UiDevice.openTheFirstNote() {
        click(displayWidth / 2, (displayHeight * 0.25).toInt())
    }
}
