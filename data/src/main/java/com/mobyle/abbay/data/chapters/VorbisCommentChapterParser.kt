package com.mobyle.abbay.data.chapters

/**
 * `CHAPTERxxx=HH:MM:SS.mmm` / `CHAPTERxxxNAME=` Vorbis comments in Ogg Vorbis,
 * Ogg Opus (`OpusTags`, RFC 7845 §5.2) and FLAC (`VORBIS_COMMENT` metadata block).
 */
@Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "TooManyFunctions") // Mirrors the format's structure.
object VorbisCommentChapterParser {
    private const val HEAD_BYTES = 1024 * 1024
    private const val FLAC_VORBIS_COMMENT = 4
    private val chapterTime = Regex("""CHAPTER(\d+)=(\d+):(\d{2}):(\d{2})(?:\.(\d{1,3}))?""", RegexOption.IGNORE_CASE)
    private val chapterName = Regex("""CHAPTER(\d+)NAME=(.*)""", RegexOption.IGNORE_CASE)

    fun parse(source: ByteSource): List<ParsedChapter> {
        val head = source.read(0, HEAD_BYTES)
        val comments = when {
            Bytes.startsWith(head, "fLaC") -> flacComments(head)
            Bytes.startsWith(head, "OggS") -> oggComments(head)
            else -> null
        } ?: return emptyList()
        return chaptersFrom(comments)
    }

    fun chaptersFrom(comments: List<String>): List<ParsedChapter> {
        val starts = mutableMapOf<Int, Long>()
        val names = mutableMapOf<Int, String>()
        comments.forEach { comment ->
            chapterTime.matchEntire(comment)?.destructured?.let { (number, hours, minutes, seconds, millis) ->
                val milliseconds = millis.padEnd(3, '0').ifEmpty { "0" }.toLong()
                starts[number.toInt()] = ((hours.toLong() * 60 + minutes.toLong()) * 60 + seconds.toLong()) * 1_000 + milliseconds
            }
            chapterName.matchEntire(comment)?.destructured?.let { (number, name) -> names[number.toInt()] = name.trim() }
        }
        return starts.toSortedMap().map { (number, start) -> ParsedChapter(names[number].orEmpty(), start) }.sortedBy { it.startMs }
    }

    private fun flacComments(head: ByteArray): List<String>? {
        var offset = 4
        while (offset + 4 <= head.size) {
            val blockHeader = Bytes.u8(head, offset)
            val length = Bytes.u24(head, offset + 1)
            val bodyStart = offset + 4
            if (blockHeader and 0x7F == FLAC_VORBIS_COMMENT && bodyStart + length <= head.size) {
                return commentList(head, bodyStart)
            }
            if (blockHeader and 0x80 != 0) return null
            offset = bodyStart + length
        }
        return null
    }

    /** Reassembles the second Ogg packet (the comment header) of the first logical stream. */
    private fun oggComments(head: ByteArray): List<String>? {
        val packets = mutableListOf<ByteArray>()
        val current = java.io.ByteArrayOutputStream()
        var offset = 0
        while (offset + 27 <= head.size && Bytes.startsWith(head, "OggS", offset) && packets.size < 2) {
            val segmentCount = Bytes.u8(head, offset + 26)
            val lacingStart = offset + 27
            var dataOffset = lacingStart + segmentCount
            if (dataOffset > head.size) return null
            for (segment in 0 until segmentCount) {
                val lacing = Bytes.u8(head, lacingStart + segment)
                if (dataOffset + lacing > head.size) return null
                current.write(head, dataOffset, lacing)
                dataOffset += lacing
                if (lacing < 255) {
                    packets += current.toByteArray()
                    current.reset()
                    if (packets.size == 2) break
                }
            }
            offset = dataOffset
        }
        val commentPacket = packets.getOrNull(1) ?: return null
        return when {
            Bytes.startsWith(commentPacket, "OpusTags") -> commentList(commentPacket, 8)
            commentPacket.size > 7 && Bytes.u8(commentPacket, 0) == 3 && Bytes.startsWith(commentPacket, "vorbis", 1) ->
                commentList(commentPacket, 7)
            else -> null
        }
    }

    /** vendor length + vendor, comment count, then length-prefixed UTF-8 `KEY=value` strings (little-endian). */
    private fun commentList(data: ByteArray, start: Int): List<String>? {
        if (start + 4 > data.size) return null
        var offset = start + 4 + Bytes.u32Le(data, start).toInt()
        if (offset + 4 > data.size) return null
        val count = Bytes.u32Le(data, offset).toInt()
        offset += 4
        return buildList {
            repeat(count) {
                if (offset + 4 > data.size) return@buildList
                val length = Bytes.u32Le(data, offset).toInt()
                offset += 4
                if (length < 0 || offset + length > data.size) return@buildList
                add(String(data, offset, length, Charsets.UTF_8))
                offset += length
            }
        }
    }
}
