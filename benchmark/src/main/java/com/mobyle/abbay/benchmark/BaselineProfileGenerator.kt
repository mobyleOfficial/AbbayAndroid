package com.mobyle.abbay.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mobyle.abbay.benchmark.AbbayJourneys.openAndCloseThePlayer
import com.mobyle.abbay.benchmark.AbbayJourneys.scrollLibrary
import com.mobyle.abbay.benchmark.AbbayJourneys.waitForLibrary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupLibraryAndPlayer() = rule.collect(packageName = AbbayJourneys.PACKAGE_NAME, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        waitForLibrary()
        scrollLibrary()
        openAndCloseThePlayer()
    }
}
