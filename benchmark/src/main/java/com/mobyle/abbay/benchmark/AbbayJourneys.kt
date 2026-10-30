package com.mobyle.abbay.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until

internal object AbbayJourneys {
    const val PACKAGE_NAME = "com.mobyle.abbay"
    private const val WAIT_MS = 5_000L
    private const val SETTLE_MS = 800L

    fun MacrobenchmarkScope.waitForLibrary() {
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), WAIT_MS)
        device.waitForIdle()
    }

    fun MacrobenchmarkScope.scrollLibrary() {
        val list = device.findObject(By.scrollable(true))
        if (list == null) {
            val centerX = device.displayWidth / 2
            device.swipe(centerX, device.displayHeight * 2 / THIRDS, centerX, device.displayHeight / THIRDS, SWIPE_STEPS)
            device.waitForIdle()
            return
        }
        list.setGestureMargin(device.displayWidth / GESTURE_MARGIN_DIVISOR)
        repeat(2) {
            list.fling(Direction.DOWN)
            device.waitForIdle()
        }
        list.fling(Direction.UP)
        device.waitForIdle()
    }

    fun MacrobenchmarkScope.openAndCloseThePlayer() {
        val centerX = device.displayWidth / 2
        val bottom = device.displayHeight - device.displayHeight / BOTTOM_SHEET_DIVISOR
        device.swipe(centerX, bottom, centerX, device.displayHeight / QUARTERS, SWIPE_STEPS)
        device.waitForIdle()
        Thread.sleep(SETTLE_MS)
        device.pressBack()
        device.waitForIdle()
    }

    private const val GESTURE_MARGIN_DIVISOR = 5
    private const val BOTTOM_SHEET_DIVISOR = 14
    private const val SWIPE_STEPS = 20
    private const val THIRDS = 3
    private const val QUARTERS = 4
}
