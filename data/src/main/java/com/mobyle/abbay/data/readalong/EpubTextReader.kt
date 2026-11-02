package com.mobyle.abbay.data.readalong

import android.content.Context
import android.net.Uri
import android.text.Html
import com.readalong.ReadAlongBookText
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads an EPUB's text for read-along: its chapters in reading order (the spine), each paragraph a paragraph, and the
 * language it declares. Markup, styles and images are dropped; what an EPUB holds besides the story (a table of
 * contents, a copyright page) is read too, and simply never matches the audio. The last book read is kept, since the
 * player asks for the same one again and again.
 */
@Singleton
class EpubTextReader @Inject constructor(@ApplicationContext private val context: Context) {
    private val lock = Mutex()

    /** The last few books read, most recent last: switching between them reads nothing again. */
    private val cached = LinkedHashMap<String, ReadAlongBookText?>()

    /** Null when the file can't be opened or isn't an EPUB with text. */
    suspend fun read(uri: String): ReadAlongBookText? = lock.withLock {
        if (cached.containsKey(uri)) return@withLock cached.remove(uri).also { cached[uri] = it }
        val text = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use(::parse) }.getOrNull()
        }
        cached[uri] = text
        if (cached.size > CACHED_BOOKS) cached.remove(cached.keys.first())
        text
    }

    companion object {
        /** The EPUB in [input], read whole. */
        internal fun parse(input: InputStream): ReadAlongBookText? {
            val entries = textEntries(input)
            val packagePath = entries[CONTAINER]?.let { ROOTFILE.find(String(it))?.groupValues?.get(1) }
            val book = packagePath?.let { entries[it] }?.let { PackageDocument.parse(String(it)) } ?: return null
            val folder = packagePath.substringBeforeLast('/', missingDelimiterValue = "")
            val paragraphs = book.spine.flatMap { href -> entries[resolve(folder, href)]?.let { paragraphsOf(String(it)) }.orEmpty() }
            return ReadAlongBookText.of(book.language, paragraphs).takeIf { it.words.isNotEmpty() }
        }

        /** The archive's text files by path, up to [MAX_TEXT_BYTES] in all; images and fonts are skipped unread. */
        private fun textEntries(input: InputStream): Map<String, ByteArray> {
            val entries = HashMap<String, ByteArray>()
            var total = 0L
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }
                    .filter { entry -> !entry.isDirectory && TEXT_EXTENSIONS.any { entry.name.endsWith(it, ignoreCase = true) } }
                    .takeWhile { total <= MAX_TEXT_BYTES }
                    .forEach { entry ->
                        val bytes = zip.readBytes()
                        total += bytes.size
                        entries[entry.name] = bytes
                    }
            }
            return entries
        }

        /** A chapter's paragraphs: its body as text, a paragraph per line. */
        private fun paragraphsOf(html: String): List<String> {
            val body = html.replace(UNSHOWN, " ")
            val text = Html.fromHtml(body, Html.FROM_HTML_MODE_LEGACY).toString()
            return text.split('\n').map { it.replace(OBJECT_MARK, ' ').trim() }.filter { it.isNotEmpty() }
        }

        private fun resolve(folder: String, href: String): String {
            val path = URLDecoder.decode(href.substringBefore('#'), Charsets.UTF_8.name())
            val parts = ArrayList<String>()
            for (part in (if (folder.isEmpty()) path else "$folder/$path").split('/')) {
                when (part) {
                    "", "." -> Unit
                    ".." -> parts.removeLastOrNull()
                    else -> parts += part
                }
            }
            return parts.joinToString("/")
        }

        private const val CONTAINER = "META-INF/container.xml"
        private const val CACHED_BOOKS = 4
        private const val MAX_TEXT_BYTES = 64L * 1024 * 1024
        private const val OBJECT_MARK = '￼'
        private val TEXT_EXTENSIONS = listOf(".xml", ".opf", ".xhtml", ".html", ".htm")
        private val ROOTFILE = Regex("""full-path\s*=\s*["']([^"']+)["']""")

        /** The head, scripts and styles: Html would show a style's rules as text. */
        private val UNSHOWN = Regex("""(?is)<head[\s>].*?</head>|<script[\s>].*?</script>|<style[\s>].*?</style>""")
    }
}
