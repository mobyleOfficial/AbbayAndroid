package com.mobyle.abbay.usecase

import com.mobyle.abbay.fakes.FakeLibraryRepository
import com.mobyle.abbay.fakes.FakeSettingsRepository
import com.settings.AppSettings
import com.settings.RescanOnStart
import com.usecase.RefreshLibraryOnStart
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshLibraryOnStartTest {
    @Test
    fun scansAsTheSettingSays() = runTest {
        val expectedCalls = mapOf(
            RescanOnStart.OFF to listOf("validateRoots"),
            RescanOnStart.QUICK to listOf("validateRoots", "quickScan"),
            RescanOnStart.FULL to listOf("validateRoots", "requestFullScan"),
        )
        expectedCalls.forEach { (setting, calls) ->
            val library = FakeLibraryRepository()
            val settings = FakeSettingsRepository(AppSettings(rescanOnStart = setting))

            RefreshLibraryOnStart(library, settings)()

            assertEquals(setting.name, calls, library.calls)
        }
    }
}
