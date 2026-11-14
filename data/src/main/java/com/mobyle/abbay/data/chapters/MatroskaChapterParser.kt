package com.mobyle.abbay.data.chapters

@Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "TooManyFunctions")
object MatroskaChapterParser {
    private const val ID_EBML = 0x1A45DFA3L
    private const val ID_SEGMENT = 0x18538067L
    private const val ID_SEEK_HEAD = 0x114D9B74L
    private const val ID_SEEK = 0x4DBBL
    private const val ID_SEEK_ID = 0x53ABL
    private const val ID_SEEK_POSITION = 0x53ACL
    private const val ID_CHAPTERS = 0x1043A770L
    private const val ID_EDITION_ENTRY = 0x45B9L
    private const val ID_CHAPTER_ATOM = 0xB6L
    private const val ID_CHAPTER_TIME_START = 0x91L
    private const val ID_CHAPTER_FLAG_HIDDEN = 0x98L
    private const val ID_CHAPTER_DISPLAY = 0x80L
    private const val ID_CHAP_STRING = 0x85L
    private const val ID_CLUSTER = 0x1F43B675L
    private const val NANOS_PER_MILLI = 1_000_000L
    private const val MAX_ELEMENT = 4 * 1024 * 1024

    private data class Element(val id: Long, val dataStart: Long, val dataSize: Long) {
        val end get() = dataStart + dataSize
    }

    fun parse(source: ByteSource): List<ParsedChapter> {
        val ebml = readElement(source, 0) ?: return emptyList()
        if (ebml.id != ID_EBML) return emptyList()
        val segment = readElement(source, ebml.end) ?: return emptyList()
        if (segment.id != ID_SEGMENT) return emptyList()
        val segmentEnd = if (segment.dataSize < 0) source.size else minOf(source.size, segment.end)

        val chapters = chaptersFromSeekHead(source, segment, segmentEnd) ?: chaptersByScan(source, segment.dataStart, segmentEnd)
            ?: return emptyList()
        if (chapters.dataSize > MAX_ELEMENT) return emptyList()
        val data = source.read(chapters.dataStart, chapters.dataSize.toInt())
        val edition = children(data, 0, data.size).firstOrNull { it.id == ID_EDITION_ENTRY } ?: return emptyList()
        return children(data, edition.dataStart.toInt(), edition.end.toInt())
            .filter { it.id == ID_CHAPTER_ATOM }
            .mapNotNull { atom -> chapterOf(data, atom) }
            .sortedBy { it.startMs }
    }

    private fun chapterOf(data: ByteArray, atom: Element): ParsedChapter? {
        var startNanos: Long? = null
        var title = ""
        var isHidden = false
        children(data, atom.dataStart.toInt(), atom.end.toInt()).forEach { element ->
            when (element.id) {
                ID_CHAPTER_TIME_START -> startNanos = unsigned(data, element)
                ID_CHAPTER_FLAG_HIDDEN -> isHidden = unsigned(data, element) == 1L
                ID_CHAPTER_DISPLAY -> if (title.isEmpty()) {
                    children(data, element.dataStart.toInt(), element.end.toInt()).firstOrNull { it.id == ID_CHAP_STRING }
                        ?.let { title = String(data, it.dataStart.toInt(), it.dataSize.toInt(), Charsets.UTF_8) }
                }
            }
        }
        val start = startNanos ?: return null
        return if (isHidden) null else ParsedChapter(title, start / NANOS_PER_MILLI)
    }

    private fun chaptersFromSeekHead(source: ByteSource, segment: Element, segmentEnd: Long): Element? {
        val seekHead = readElement(source, segment.dataStart)?.takeIf { it.id == ID_SEEK_HEAD && it.dataSize in 1..MAX_ELEMENT }
            ?: return null
        val data = source.read(seekHead.dataStart, seekHead.dataSize.toInt())
        children(data, 0, data.size).filter { it.id == ID_SEEK }.forEach { seek ->
            val fields = children(data, seek.dataStart.toInt(), seek.end.toInt())
            val seekId = fields.firstOrNull { it.id == ID_SEEK_ID }?.let { unsigned(data, it) }
            val position = fields.firstOrNull { it.id == ID_SEEK_POSITION }?.let { unsigned(data, it) }
            if (seekId == ID_CHAPTERS && position != null && segment.dataStart + position < segmentEnd) {
                return readElement(source, segment.dataStart + position)?.takeIf { it.id == ID_CHAPTERS }
            }
        }
        return null
    }

    private fun chaptersByScan(source: ByteSource, from: Long, until: Long): Element? {
        var position = from
        while (position < until) {
            val element = readElement(source, position) ?: return null
            if (element.id == ID_CHAPTERS) return element
            if (element.id == ID_CLUSTER || element.dataSize < 0) return null
            position = element.end
        }
        return null
    }

    private fun readElement(source: ByteSource, position: Long): Element? {
        val header = source.read(position, 16)
        val (id, idLength) = vint(header, 0, keepMarker = true) ?: return null
        val (size, sizeLength) = vint(header, idLength, keepMarker = false) ?: return null
        return Element(id, position + idLength + sizeLength, size)
    }

    private fun children(data: ByteArray, from: Int, until: Int): List<Element> {
        val elements = mutableListOf<Element>()
        var position = from
        while (position < until) {
            val (id, idLength) = vint(data, position, keepMarker = true) ?: break
            val (size, sizeLength) = vint(data, position + idLength, keepMarker = false) ?: break
            val dataStart = position + idLength + sizeLength
            if (size < 0 || dataStart + size > until) break
            elements += Element(id, dataStart.toLong(), size)
            position = (dataStart + size).toInt()
        }
        return elements
    }

    private fun vint(data: ByteArray, offset: Int, keepMarker: Boolean): Pair<Long, Int>? {
        if (offset >= data.size) return null
        val first = Bytes.u8(data, offset)
        val length = (0 until 8).firstOrNull { first and (0x80 shr it) != 0 }?.plus(1) ?: return null
        if (offset + length > data.size) return null
        var value = if (keepMarker) first.toLong() else (first and (0xFF shr length)).toLong()
        for (index in 1 until length) value = (value shl 8) or Bytes.u8(data, offset + index).toLong()
        val allOnes = (1L shl (7 * length)) - 1
        return (if (!keepMarker && value == allOnes) -1L else value) to length
    }

    private fun unsigned(data: ByteArray, element: Element): Long =
        (0 until element.dataSize.toInt()).fold(0L) { value, index ->
            (value shl 8) or Bytes.u8(data, element.dataStart.toInt() + index).toLong()
        }
}
