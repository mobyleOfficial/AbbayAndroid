package com.mobyle.abbay.data.chapters

@Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "TooManyFunctions")
object Id3ChapterParser {
    private const val HEADER_SIZE = 10
    private const val FLAG_UNSYNCHRONISATION = 0x80
    private const val FLAG_EXTENDED_HEADER = 0x40
    private const val CTOC_FLAG_TOP_LEVEL = 0x02
    private const val MAX_TAG_SIZE = 16 * 1024 * 1024

    private data class ChapterFrame(val elementId: String, val startMs: Long, val title: String)

    fun parse(source: ByteSource): List<ParsedChapter> {
        val header = source.read(0, HEADER_SIZE)
        if (header.size < HEADER_SIZE || !Bytes.startsWith(header, "ID3")) return emptyList()
        val majorVersion = Bytes.u8(header, 3)
        if (majorVersion !in 3..4) return emptyList()
        val flags = Bytes.u8(header, 5)
        val tagSize = syncSafe(header, 6)
        if (tagSize <= 0 || tagSize > MAX_TAG_SIZE) return emptyList()

        var tag = source.read(HEADER_SIZE.toLong(), tagSize)
        if (majorVersion == 3 && flags and FLAG_UNSYNCHRONISATION != 0) tag = removeUnsynchronisation(tag)
        var offset = if (flags and FLAG_EXTENDED_HEADER != 0) extendedHeaderSize(tag, majorVersion) else 0

        val chapters = mutableListOf<ChapterFrame>()
        var topLevelOrder: List<String>? = null
        while (offset + HEADER_SIZE <= tag.size) {
            val frameId = Bytes.ascii(tag, offset, 4)
            if (frameId[0] == '\u0000') break
            val frameSize = if (majorVersion == 4) syncSafe(tag, offset + 4) else Bytes.u32(tag, offset + 4).toInt()
            val bodyStart = offset + HEADER_SIZE
            if (frameSize <= 0 || bodyStart + frameSize > tag.size) break
            val body = tag.copyOfRange(bodyStart, bodyStart + frameSize)
            when (frameId) {
                "CHAP" -> parseChap(body, majorVersion)?.let(chapters::add)
                "CTOC" -> parseCtoc(body)?.let { if (topLevelOrder == null) topLevelOrder = it }
            }
            offset = bodyStart + frameSize
        }

        val ordered = topLevelOrder?.mapNotNull { id -> chapters.firstOrNull { it.elementId == id } }?.takeIf { it.isNotEmpty() }
            ?: chapters.sortedBy { it.startMs }
        return ordered.map { ParsedChapter(it.title, it.startMs) }
    }

    private fun parseChap(body: ByteArray, majorVersion: Int): ChapterFrame? {
        val idEnd = body.indexOf(0)
        if (idEnd < 0 || idEnd + 17 > body.size) return null
        val elementId = Bytes.ascii(body, 0, idEnd)
        val startMs = Bytes.u32(body, idEnd + 1)
        var offset = idEnd + 17
        var title = ""
        while (offset + HEADER_SIZE <= body.size) {
            val subId = Bytes.ascii(body, offset, 4)
            val subSize = if (majorVersion == 4) syncSafe(body, offset + 4) else Bytes.u32(body, offset + 4).toInt()
            val subStart = offset + HEADER_SIZE
            if (subSize <= 0 || subStart + subSize > body.size) break
            if (subId == "TIT2") title = decodeTextFrame(body.copyOfRange(subStart, subStart + subSize))
            offset = subStart + subSize
        }
        return ChapterFrame(elementId, startMs, title)
    }

    private fun parseCtoc(body: ByteArray): List<String>? {
        val idEnd = body.indexOf(0)
        if (idEnd < 0 || idEnd + 2 >= body.size) return null
        val flags = Bytes.u8(body, idEnd + 1)
        if (flags and CTOC_FLAG_TOP_LEVEL == 0) return null
        val entryCount = Bytes.u8(body, idEnd + 2)
        var offset = idEnd + 3
        return buildList {
            repeat(entryCount) {
                val end = (offset until body.size).firstOrNull { body[it] == 0.toByte() } ?: return@buildList
                add(Bytes.ascii(body, offset, end - offset))
                offset = end + 1
            }
        }
    }

    private fun decodeTextFrame(frame: ByteArray): String {
        if (frame.isEmpty()) return ""
        val charset = when (Bytes.u8(frame, 0)) {
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        return String(frame, 1, frame.size - 1, charset).trimEnd('\u0000').trim()
    }

    private fun extendedHeaderSize(tag: ByteArray, majorVersion: Int): Int {
        if (tag.size < 4) return 0
        return if (majorVersion == 4) syncSafe(tag, 0) else Bytes.u32(tag, 0).toInt() + 4
    }

    private fun syncSafe(data: ByteArray, offset: Int): Int =
        (0 until 4).fold(0) { size, index -> (size shl 7) or (Bytes.u8(data, offset + index) and 0x7F) }

    private fun removeUnsynchronisation(data: ByteArray): ByteArray {
        val output = java.io.ByteArrayOutputStream(data.size)
        var index = 0
        while (index < data.size) {
            output.write(data[index].toInt())
            if (Bytes.u8(data, index) == 0xFF && index + 1 < data.size && data[index + 1] == 0.toByte()) index++
            index++
        }
        return output.toByteArray()
    }

    private fun ByteArray.indexOf(value: Int): Int = indexOfFirst { it.toInt() == value }
}
