package com.mobyle.abbay.presentation.about

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class LicenseNoticesTest {
    @Test
    fun parsesCreditsAndLibraries() {
        val notices = LicenseNotices.parse(
            """
            {"bundled":[{"name":"RNNoise","source":"Xiph","license":"BSD-3-Clause"}],
             "assets":[],
             "libraries":[{"name":"androidx.core:core","version":"1.13.1","license":"Apache License, Version 2.0",
                           "url":"https://www.apache.org/licenses/LICENSE-2.0"},
                          {"name":"no.url:lib","version":"","license":"MIT","url":""}]}
            """.trimIndent(),
        )

        assertEquals(listOf(LicenseNotice("RNNoise", "Xiph · BSD-3-Clause", null)), notices.bundled)
        assertEquals(emptyList<LicenseNotice>(), notices.assets)
        assertEquals(
            listOf(
                LicenseNotice("androidx.core:core 1.13.1", "Apache License, Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
                LicenseNotice("no.url:lib", "MIT", null),
            ),
            notices.libraries,
        )
    }

    @Test
    fun theBuildPutsTheNoticesIntoTheAssets() {
        val notices = LicenseNotices.read(ApplicationProvider.getApplicationContext())

        assertTrue(notices.libraries.any { it.name.startsWith("androidx.media3:media3-exoplayer") })
        assertTrue(notices.bundled.any { it.name.startsWith("RNNoise") })
    }
}
