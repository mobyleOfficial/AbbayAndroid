package com.mobyle.abbay.data.chapters

/**
 * MP4 / M4B chapters (ISO/IEC 14496-12 box structure): the QuickTime chapter text track
 * referenced by `tref/chap`, otherwise the Nero `udta/chpl` box. Boxes are read by position, so the large sample
 * tables of the audio track are never loaded.
 */
@Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "TooManyFunctions") // Mirrors the format's structure.
object Mp4ChapterParser {
    private const val BOX_HEADER = 8
    private const val NERO_UNITS_PER_MS = 10_000L
    private const val MAX_LEAF_BOX = 4 * 1024 * 1024

    private data class Box(val type: String, val start: Long, val headerSize: Int, val size: Long) {
        val contentStart get() = start + headerSize
        val end get() = start + size
    }

    fun parse(source: ByteSource): List<ParsedChapter> {
        val moov = children(source, 0, source.size).firstOrNull { it.type == "moov" } ?: return emptyList()
        val tracks = children(source, moov.contentStart, moov.end).filter { it.type == "trak" }
        return quickTimeChapters(source, tracks).ifEmpty { neroChapters(source, moov) }
    }

    private fun children(source: ByteSource, from: Long, until: Long): List<Box> {
        val boxes = mutableListOf<Box>()
        var position = from
        while (position + BOX_HEADER <= until) {
            val header = source.read(position, 16)
            if (header.size < BOX_HEADER) break
            var size = Bytes.u32(header, 0)
            var headerSize = BOX_HEADER
            if (size == 1L && header.size >= 16) {
                size = Bytes.u64(header, 8)
                headerSize = 16
            } else if (size == 0L) {
                size = until - position
            }
            if (size < headerSize || position + size > until) break
            boxes += Box(Bytes.ascii(header, 4, 4), position, headerSize, size)
            position += size
        }
        return boxes
    }

    private fun child(source: ByteSource, parent: Box, vararg path: String): Box? {
        var current: Box = parent
        path.forEach { type ->
            current = children(source, current.contentStart, current.end).firstOrNull { it.type == type } ?: return null
        }
        return current
    }

    private fun content(source: ByteSource, box: Box): ByteArray =
        if (box.size - box.headerSize > MAX_LEAF_BOX) ByteArray(0) else source.read(box.contentStart, (box.size - box.headerSize).toInt())

    /** Nero `chpl`: FullBox, a reserved 32-bit field (version 1), a chapter count, then 100 ns start + Pascal title. */
    private fun neroChapters(source: ByteSource, moov: Box): List<ParsedChapter> {
        val chpl = child(source, moov, "udta", "chpl") ?: return emptyList()
        val data = content(source, chpl)
        if (data.size < 5) return emptyList()
        var offset = 4 + if (Bytes.u8(data, 0) >= 1) 4 else 0
        if (offset >= data.size) return emptyList()
        val count = Bytes.u8(data, offset++)
        return buildList {
            repeat(count) {
                if (offset + 9 > data.size) return@buildList
                val start = Bytes.u64(data, offset) / NERO_UNITS_PER_MS
                val titleLength = Bytes.u8(data, offset + 8)
                offset += 9
                if (offset + titleLength > data.size) return@buildList
                add(ParsedChapter(String(data, offset, titleLength, Charsets.UTF_8), start))
                offset += titleLength
            }
        }
    }

    /** The text track named by an audio track's `tref/chap`: one sample per chapter, durations from `stts`. */
    private fun quickTimeChapters(source: ByteSource, tracks: List<Box>): List<ParsedChapter> {
        val chapterTrackIds = tracks.mapNotNull { child(source, it, "tref", "chap") }
            .flatMap { chap -> content(source, chap).let { data -> (0 until data.size / 4).map { Bytes.u32(data, it * 4) } } }
            .toSet()
        val chapterTrack = tracks.firstOrNull { trackId(source, it) in chapterTrackIds } ?: return emptyList()

        val timescale = child(source, chapterTrack, "mdia", "mdhd")?.let { mediaTimescale(content(source, it)) } ?: return emptyList()
        val stbl = child(source, chapterTrack, "mdia", "minf", "stbl") ?: return emptyList()
        val durations = child(source, stbl, "stts")?.let { sampleDurations(content(source, it)) } ?: return emptyList()
        val sizes = child(source, stbl, "stsz")?.let { sampleSizes(content(source, it), durations.size) } ?: return emptyList()
        val chunkOffsets = (child(source, stbl, "stco") ?: child(source, stbl, "co64"))
            ?.let { chunkOffsets(content(source, it), it.type == "co64") } ?: return emptyList()
        val samplesPerChunk = child(source, stbl, "stsc")?.let { samplesPerChunk(content(source, it), chunkOffsets.size) }
            ?: return emptyList()

        val sampleOffsets = mutableListOf<Long>()
        var sampleIndex = 0
        chunkOffsets.forEachIndexed { chunk, chunkOffset ->
            var offset = chunkOffset
            repeat(samplesPerChunk[chunk]) {
                if (sampleIndex < sizes.size) {
                    sampleOffsets += offset
                    offset += sizes[sampleIndex]
                    sampleIndex++
                }
            }
        }

        var startUnits = 0L
        return sampleOffsets.indices.map { index ->
            val sample = source.read(sampleOffsets[index], sizes[index].toInt())
            val textLength = if (sample.size >= 2) Bytes.u16(sample, 0) else 0
            val title = if (textLength in 1..(sample.size - 2)) decodeText(sample, 2, textLength) else ""
            ParsedChapter(title, startUnits * 1_000 / timescale).also { startUnits += durations.getOrElse(index) { 0L } }
        }
    }

    private fun trackId(source: ByteSource, track: Box): Long? {
        val data = child(source, track, "tkhd")?.let { content(source, it) } ?: return null
        val idOffset = if (Bytes.u8(data, 0) == 1) 4 + 16 else 4 + 8
        return if (data.size >= idOffset + 4) Bytes.u32(data, idOffset) else null
    }

    private fun mediaTimescale(data: ByteArray): Long? {
        val offset = if (Bytes.u8(data, 0) == 1) 4 + 16 else 4 + 8
        return if (data.size >= offset + 4) Bytes.u32(data, offset).takeIf { it > 0 } else null
    }

    private fun sampleDurations(data: ByteArray): List<Long> {
        val entries = Bytes.u32(data, 4).toInt()
        return buildList {
            for (entry in 0 until entries) {
                val offset = 8 + entry * 8
                if (offset + 8 > data.size) break
                val count = Bytes.u32(data, offset).toInt()
                val delta = Bytes.u32(data, offset + 4)
                repeat(count) { add(delta) }
            }
        }
    }

    private fun sampleSizes(data: ByteArray, sampleCount: Int): List<Long> {
        val uniformSize = Bytes.u32(data, 4)
        val count = Bytes.u32(data, 8).toInt()
        return if (uniformSize != 0L) {
            List(maxOf(count, sampleCount)) { uniformSize }
        } else {
            (0 until count).filter { 12 + it * 4 + 4 <= data.size }.map { Bytes.u32(data, 12 + it * 4) }
        }
    }

    private fun chunkOffsets(data: ByteArray, isLarge: Boolean): List<Long> {
        val count = Bytes.u32(data, 4).toInt()
        val width = if (isLarge) 8 else 4
        return (0 until count).filter { 8 + it * width + width <= data.size }
            .map { if (isLarge) Bytes.u64(data, 8 + it * 8) else Bytes.u32(data, 8 + it * 4) }
    }

    /** Expands `stsc` runs (first chunk, samples per chunk) to one value per chunk. */
    private fun samplesPerChunk(data: ByteArray, chunkCount: Int): List<Int> {
        val entries = Bytes.u32(data, 4).toInt()
        val runs = (0 until entries).filter { 8 + it * 12 + 12 <= data.size }
            .map { Bytes.u32(data, 8 + it * 12).toInt() to Bytes.u32(data, 8 + it * 12 + 4).toInt() }
        return List(chunkCount) { chunkIndex ->
            runs.lastOrNull { (firstChunk, _) -> firstChunk <= chunkIndex + 1 }?.second ?: 1
        }
    }

    /** QuickTime text samples are UTF-8, or UTF-16 when they start with a byte-order mark. */
    private fun decodeText(data: ByteArray, offset: Int, length: Int): String {
        val first = if (length >= 2) Bytes.u8(data, offset) else 0
        val second = if (length >= 2) Bytes.u8(data, offset + 1) else 0
        val isUtf16 = (first == 0xFE && second == 0xFF) || (first == 0xFF && second == 0xFE)
        return String(data, offset, length, if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8).trim()
    }
}
