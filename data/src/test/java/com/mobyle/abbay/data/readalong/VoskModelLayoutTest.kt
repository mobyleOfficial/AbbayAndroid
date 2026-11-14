package com.mobyle.abbay.data.readalong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VoskModelLayoutTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a first-layout model moves to the current layout with the tuned settings`() {
        val model = folder.newFolder("pt")
        val files = listOf("final.mdl", "mfcc.conf", "HCLr.fst", "Gr.fst", "disambig_tid.int", "word_boundary.int")
        files.forEach { name -> File(model, name).writeText(name) }
        File(model, "ivector").mkdirs()

        VoskModelLayout.modernise(model)

        assertEquals("final.mdl", File(model, "am/final.mdl").readText())
        assertEquals("word_boundary.int", File(model, "graph/phones/word_boundary.int").readText())
        assertTrue(File(model, "graph/HCLr.fst").isFile)
        assertTrue(File(model, "conf/model.conf").readText().contains("--max-active=3000"))
        assertFalse(File(model, "final.mdl").exists())
        assertTrue(File(model, "ivector").isDirectory)
    }

    @Test
    fun `a current-layout model is left as it is`() {
        val model = folder.newFolder("en")
        File(model, "am").mkdirs()
        File(model, "am/final.mdl").writeText("model")
        File(model, "conf").mkdirs()
        File(model, "conf/model.conf").writeText("--beam=9.0\n")

        VoskModelLayout.modernise(model)

        assertEquals("--beam=9.0\n", File(model, "conf/model.conf").readText())
    }
}
