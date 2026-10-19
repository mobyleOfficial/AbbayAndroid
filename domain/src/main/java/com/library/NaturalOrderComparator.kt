package com.library

import java.text.Collator
import java.util.Locale

/**
 * Natural sort: names are split into digit and non-digit runs; digit runs compare
 * by numeric value, other runs with a PRIMARY-strength collator, treating `_`, `-` and `.` as spaces.
 */
class NaturalOrderComparator(locale: Locale = Locale.getDefault()) : Comparator<String> {
    private val collator: Collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }

    override fun compare(first: String, second: String): Int {
        val firstRuns = runsOf(first)
        val secondRuns = runsOf(second)

        for (index in 0 until minOf(firstRuns.size, secondRuns.size)) {
            val comparison = compareRuns(firstRuns[index], secondRuns[index])
            if (comparison != 0) return comparison
        }

        val lengthComparison = firstRuns.size.compareTo(secondRuns.size)
        return if (lengthComparison != 0) lengthComparison else first.compareTo(second)
    }

    private fun compareRuns(firstRun: String, secondRun: String): Int {
        val firstIsDigits = firstRun.first().isDigit()
        val secondIsDigits = secondRun.first().isDigit()

        return when {
            firstIsDigits && secondIsDigits -> compareDigitRuns(firstRun, secondRun)
            firstIsDigits -> -1
            secondIsDigits -> 1
            else -> collator.compare(normalizeText(firstRun), normalizeText(secondRun))
        }
    }

    private fun compareDigitRuns(firstRun: String, secondRun: String): Int {
        val firstDigits = firstRun.trimStart('0')
        val secondDigits = secondRun.trimStart('0')
        val lengthComparison = firstDigits.length.compareTo(secondDigits.length)

        return if (lengthComparison != 0) lengthComparison else firstDigits.compareTo(secondDigits)
    }

    private fun normalizeText(run: String): String = run.replace(SEPARATORS, " ").trim()

    private fun runsOf(name: String): List<String> {
        val runs = mutableListOf<String>()
        val currentRun = StringBuilder()

        name.forEach { character ->
            if (currentRun.isNotEmpty() && currentRun.last().isDigit() != character.isDigit()) {
                runs += currentRun.toString()
                currentRun.clear()
            }
            currentRun.append(character)
        }
        if (currentRun.isNotEmpty()) runs += currentRun.toString()

        return runs
    }

    private companion object {
        val SEPARATORS = Regex("[_.\\-]")
    }
}
