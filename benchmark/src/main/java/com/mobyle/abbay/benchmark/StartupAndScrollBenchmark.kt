package com.mobyle.abbay.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mobyle.abbay.benchmark.AbbayJourneys.scrollLibrary
import com.mobyle.abbay.benchmark.AbbayJourneys.waitForLibrary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Performance budgets: cold start ≤ 800 ms; library scrolling with ≤ 2 % janky frames. */
@RunWith(AndroidJUnit4::class)
class StartupAndScrollBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartWithoutProfile() = coldStart(CompilationMode.None())

    @Test
    fun coldStartWithBaselineProfile() = coldStart(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun libraryScroll() = rule.measureRepeated(
        packageName = AbbayJourneys.PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        setupBlock = {
            startActivityAndWait()
            waitForLibrary()
        },
    ) { scrollLibrary() }

    private fun coldStart(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = AbbayJourneys.PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        iterations = ITERATIONS,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        waitForLibrary()
    }

    private companion object {
        const val ITERATIONS = 5
    }
}
