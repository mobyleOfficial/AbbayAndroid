package com.mobyle.abbay.presentation.readalong

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadAlongTextToggleTest {
    private val seen = mutableSetOf<String>()

    private fun TestScope.toggle() = ReadAlongTextToggle(this, isIntroSeen = { it in seen }, markIntroSeen = { seen += it })

    @Test
    fun `the first time a book's text is asked for, the explanation shows before it`() = runTest {
        val toggle = toggle()

        toggle.toggle("book")
        runCurrent()

        assertTrue(toggle.isIntroShown.value)
        assertFalse(toggle.showsText.value)
    }

    @Test
    fun `once read for a book, its text shows straight away, but another book still explains first`() = runTest {
        val toggle = toggle()
        toggle.toggle("book")
        runCurrent()
        toggle.confirmIntro()
        runCurrent()
        toggle.showCover()

        toggle.toggle("book")
        runCurrent()
        assertTrue(toggle.showsText.value)
        assertFalse(toggle.isIntroShown.value)

        toggle.showCover()
        toggle.toggle("other")
        runCurrent()
        assertTrue(toggle.isIntroShown.value)
        assertFalse(toggle.showsText.value)
    }

    @Test
    fun `dismissing the explanation keeps the cover and asks again next time`() = runTest {
        val toggle = toggle()
        toggle.toggle("book")
        runCurrent()

        toggle.dismissIntro()
        toggle.toggle("book")
        runCurrent()

        assertFalse(toggle.showsText.value)
        assertTrue(toggle.isIntroShown.value)
    }
}
