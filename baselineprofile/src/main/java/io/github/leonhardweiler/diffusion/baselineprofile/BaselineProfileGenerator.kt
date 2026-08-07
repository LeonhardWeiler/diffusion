package io.github.leonhardweiler.diffusion.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val packageUnderTest: String
    get() = InstrumentationRegistry.getArguments()
        .getString("androidx.benchmark.targetPackageName")
        ?: error("the plugin did not say which app is being profiled")

private const val IDLE_MS = 2_000L

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = packageUnderTest,
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()

        device.waitForIdle(IDLE_MS)
        device.scrollTheList()

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

    private fun UiDevice.openTheFirstNote() {
        click(displayWidth / 2, (displayHeight * 0.25).toInt())
    }
}
