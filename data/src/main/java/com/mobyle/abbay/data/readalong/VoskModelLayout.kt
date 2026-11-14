package com.mobyle.abbay.data.readalong

import java.io.File
import java.io.IOException

/**
 * Brings a model in Vosk's first folder layout (everything at the root, as the Portuguese and Turkish small models
 * still ship) to the current one (`am/`, `conf/`, `graph/`), which is the only layout that reads `conf/model.conf`.
 *
 * Without that file Vosk decodes with its widest search (max-active 7000, beam 13, lattice-beam 6); with the settings
 * every other small model ships ([TUNED_CONFIG]) the same audio transcribes about twice as fast on a phone (measured on
 * a Moto G9 Plus: 1.6× to 2.9× real time for Portuguese), which is what keeps the text ahead of playback.
 */
object VoskModelLayout {
    /** Where each file of the first layout goes in the current one; files a model doesn't have are skipped. */
    private val MOVES = mapOf(
        "final.mdl" to "am/final.mdl",
        "mfcc.conf" to "conf/mfcc.conf",
        "HCLG.fst" to "graph/HCLG.fst",
        "HCLr.fst" to "graph/HCLr.fst",
        "Gr.fst" to "graph/Gr.fst",
        "disambig_tid.int" to "graph/disambig_tid.int",
        "phones.txt" to "graph/phones.txt",
        "words.txt" to "graph/words.txt",
        "word_boundary.int" to "graph/phones/word_boundary.int",
    )

    /** The decoding and end-of-utterance settings of the current small models (silence and noise are phones 1–10 in all). */
    internal val TUNED_CONFIG = listOf(
        "--min-active=200",
        "--max-active=3000",
        "--beam=10.0",
        "--lattice-beam=2.0",
        "--acoustic-scale=1.0",
        "--frame-subsampling-factor=3",
        "--endpoint.silence-phones=1:2:3:4:5:6:7:8:9:10",
        "--endpoint.rule2.min-trailing-silence=0.5",
        "--endpoint.rule3.min-trailing-silence=0.75",
        "--endpoint.rule4.min-trailing-silence=1.0",
    ).joinToString("\n", postfix = "\n")

    /** Moves a first-layout model in [folder] to the current layout and gives it the tuned settings; others are left as they are. */
    fun modernise(folder: File) {
        if (!File(folder, "final.mdl").isFile || File(folder, "am/final.mdl").exists()) return
        MOVES.forEach { (from, to) ->
            val source = File(folder, from)
            if (!source.exists()) return@forEach
            val target = File(folder, to)
            target.parentFile?.mkdirs()
            if (!source.renameTo(target)) throw IOException("Could not move $from")
        }
        File(folder, "conf/model.conf").writeText(TUNED_CONFIG)
    }
}
