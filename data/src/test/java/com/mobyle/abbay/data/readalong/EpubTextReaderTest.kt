package com.mobyle.abbay.data.readalong

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class EpubTextReaderTest {
    private fun epub(files: Map<String, String>): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((name, content) in files) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }

    private val container = """
        <?xml version="1.0"?>
        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
          <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
        </container>
    """.trimIndent()

    private val opf = """
        <?xml version="1.0" encoding="UTF-8"?>
        <package xmlns="http://www.idpf.org/2007/opf" version="3.0">
          <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:language>pt-BR</dc:language></metadata>
          <manifest>
            <item id="cover" href="Text/cover.xhtml" media-type="application/xhtml+xml"/>
            <item id="one" href="Text/chapter%201.xhtml" media-type="application/xhtml+xml"/>
            <item id="two" href="Text/../Text/two.xhtml" media-type="application/xhtml+xml"/>
          </manifest>
          <spine><itemref idref="cover" linear="no"/><itemref idref="two"/><itemref idref="one"/></spine>
        </package>
    """.trimIndent()

    private fun chapter(body: String) =
        """<html><head><title>Ignored</title><style>p { margin: 0 }</style></head><body>$body</body></html>"""

    @Test
    fun `reads the chapters in spine order, a paragraph per paragraph, with the declared language`() {
        val text = EpubTextReader.parse(
            epub(
                mapOf(
                    "META-INF/container.xml" to container,
                    "OEBPS/content.opf" to opf,
                    "OEBPS/Text/cover.xhtml" to chapter("<p>Cover</p>"),
                    "OEBPS/Text/chapter 1.xhtml" to chapter("<h1>Um</h1><p>Era uma vez&nbsp;um <i>rei</i>.</p>"),
                    "OEBPS/Text/two.xhtml" to chapter("<p>Primeiro — depois.</p><p>Fim.</p>"),
                ),
            ),
        )!!

        assertEquals("pt-BR", text.language)
        assertEquals(listOf("Primeiro", "—", "depois.", "Fim.", "Um", "Era", "uma", "vez", "um", "rei."), text.words)
        assertEquals(setOf(0, 3, 4, 5), text.paragraphStarts)
    }

    @Test
    fun `a file that isn't an EPUB has no text`() {
        assertNull(EpubTextReader.parse(epub(mapOf("readme.txt" to "hello"))))
    }
}
