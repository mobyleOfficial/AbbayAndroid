package com.mobyle.abbay.data.chapters

import java.io.ByteArrayOutputStream

/** Builders for tiny synthetic media files, assembled from the public format specifications. */
object SyntheticFiles {
    fun u16(value: Int) = byteArrayOf((value shr 8).toByte(), value.toByte())

    fun u32(value: Long) = byteArrayOf((value shr 24).toByte(), (value shr 16).toByte(), (value shr 8).toByte(), value.toByte())

    fun u64(value: Long) = u32(value ushr 32) + u32(value and 0xFFFFFFFFL)

    fun u32Le(value: Int) = byteArrayOf(value.toByte(), (value shr 8).toByte(), (value shr 16).toByte(), (value shr 24).toByte())

    fun concat(vararg parts: ByteArray): ByteArray = ByteArrayOutputStream().apply { parts.forEach { write(it) } }.toByteArray()

    private fun u24(value: Int) = byteArrayOf((value shr 16).toByte(), (value shr 8).toByte(), value.toByte())

    private fun lengthPrefixed(text: String): ByteArray {
        val bytes = text.toByteArray()
        return u32Le(bytes.size) + bytes
    }

    // ISO/IEC 14496-12 boxes

    fun box(type: String, vararg content: ByteArray): ByteArray {
        val body = concat(*content)
        return concat(u32(8L + body.size), type.toByteArray(), body)
    }

    fun fullBox(type: String, version: Int, vararg content: ByteArray) = box(type, byteArrayOf(version.toByte(), 0, 0, 0), *content)

    fun neroChpl(chapters: List<Pair<Long, String>>): ByteArray {
        val entries = chapters.map { (startMs, title) ->
            val titleBytes = title.toByteArray()
            concat(u64(startMs * 10_000), byteArrayOf(titleBytes.size.toByte()), titleBytes)
        }
        return fullBox("chpl", 1, u32(0), byteArrayOf(chapters.size.toByte()), *entries.toTypedArray())
    }

    /**
     * An M4B whose audio track (id 1) references a text chapter track (id 2) through `tref/chap`. Samples are stored
     * in an `mdat` placed before `moov`; offsets in `stco` are absolute.
     */
    fun quickTimeChapterFile(chapters: List<Pair<String, Long>>, timescale: Long = 1_000): ByteArray {
        val samples = chapters.map { (title, _) ->
            val text = title.toByteArray()
            concat(u16(text.size), text)
        }
        val ftyp = box("ftyp", "M4B ".toByteArray(), u32(0))
        val firstSampleOffset = ftyp.size + 8
        val mdat = box("mdat", *samples.toTypedArray())
        val durations = chapters.map { (_, durationMs) -> durationMs * timescale / 1_000 }

        val audioTrack = box("trak", trackHeader(1), box("tref", box("chap", u32(2))))
        val stts = fullBox("stts", 0, u32(durations.size.toLong()), *durations.map { concat(u32(1), u32(it)) }.toTypedArray())
        val stsz = fullBox("stsz", 0, u32(0), u32(samples.size.toLong()), *samples.map { u32(it.size.toLong()) }.toTypedArray())
        val stsc = fullBox("stsc", 0, u32(1), u32(1), u32(samples.size.toLong()), u32(1))
        val stco = fullBox("stco", 0, u32(1), u32(firstSampleOffset.toLong()))
        val mdhd = fullBox("mdhd", 0, u32(0), u32(0), u32(timescale), u32(durations.sum()))
        val sampleTable = box("stbl", stts, stsz, stsc, stco)
        val textTrack = box("trak", trackHeader(2), box("mdia", mdhd, box("minf", sampleTable)))
        return concat(ftyp, mdat, box("moov", audioTrack, textTrack))
    }

    private fun trackHeader(trackId: Long) = fullBox("tkhd", 0, u32(0), u32(0), u32(trackId), u32(0), u32(0))

    // ID3v2 (ID3v2.3 / ID3v2.4 and the chapter frame addendum)

    private fun syncSafe(value: Int) = byteArrayOf(
        ((value shr 21) and 0x7F).toByte(),
        ((value shr 14) and 0x7F).toByte(),
        ((value shr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )

    fun id3Frame(id: String, body: ByteArray, version: Int): ByteArray {
        val size = if (version == 4) syncSafe(body.size) else u32(body.size.toLong())
        return concat(id.toByteArray(), size, byteArrayOf(0, 0), body)
    }

    fun tit2(title: String, version: Int, encoding: Int = 3): ByteArray {
        val text = when (encoding) {
            1 -> byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + title.toByteArray(Charsets.UTF_16LE)
            0 -> title.toByteArray(Charsets.ISO_8859_1)
            else -> title.toByteArray(Charsets.UTF_8)
        }
        return id3Frame("TIT2", byteArrayOf(encoding.toByte()) + text, version)
    }

    fun chap(elementId: String, startMs: Long, endMs: Long, title: String, version: Int, encoding: Int = 3): ByteArray {
        val noOffset = u32(0xFFFFFFFFL)
        val body = concat(
            elementId.toByteArray(),
            byteArrayOf(0),
            u32(startMs),
            u32(endMs),
            noOffset,
            noOffset,
            tit2(title, version, encoding),
        )
        return id3Frame("CHAP", body, version)
    }

    fun ctoc(childIds: List<String>, version: Int): ByteArray {
        val children = childIds.map { it.toByteArray() + byteArrayOf(0) }
        val body = concat("toc".toByteArray(), byteArrayOf(0, 0x03, childIds.size.toByte()), *children.toTypedArray())
        return id3Frame("CTOC", body, version)
    }

    fun id3Tag(version: Int, vararg frames: ByteArray): ByteArray {
        val body = concat(*frames, ByteArray(16))
        return concat("ID3".toByteArray(), byteArrayOf(version.toByte(), 0, 0), syncSafe(body.size), body)
    }

    // Matroska / EBML

    private fun ebmlSize(size: Int): ByteArray = when {
        size < 0x7F -> byteArrayOf((0x80 or size).toByte())
        size < 0x3FFF -> byteArrayOf((0x40 or (size shr 8)).toByte(), size.toByte())
        else -> byteArrayOf(0x10, (size shr 16).toByte(), (size shr 8).toByte(), size.toByte())
    }

    private fun idBytes(id: Long): ByteArray =
        generateSequence(id) { it shr 8 }.takeWhile { it > 0 }.map { it.toByte() }.toList().reversed().toByteArray()

    fun element(id: Long, vararg content: ByteArray): ByteArray {
        val body = concat(*content)
        return concat(idBytes(id), ebmlSize(body.size), body)
    }

    fun uintElement(id: Long, value: Long): ByteArray {
        val significant = u64(value).dropWhile { it == 0.toByte() }.toByteArray()
        return element(id, if (significant.isEmpty()) byteArrayOf(0) else significant)
    }

    fun matroska(chapters: List<Pair<Long, String>>, withSeekHead: Boolean): ByteArray {
        val atoms = chapters.mapIndexed { index, (startMs, title) ->
            element(
                0xB6,
                uintElement(0x73C4, index + 1L),
                uintElement(0x91, startMs * 1_000_000),
                element(0x80, element(0x85, title.toByteArray())),
            )
        }
        val chaptersElement = element(0x1043A770, element(0x45B9, *atoms.toTypedArray()))
        val info = element(0x1549A966, uintElement(0x2AD7B1, 1_000_000))
        val segmentBody = if (withSeekHead) {
            // SeekHead first; Chapters after an Info element. Positions are relative to the segment data start.
            val seekHead = { position: Long ->
                element(0x114D9B74, element(0x4DBB, element(0x53AB, idBytes(0x1043A770)), uintElement(0x53AC, position)))
            }
            val seekHeadSize = seekHead(0x1000).size
            concat(seekHead((seekHeadSize + info.size).toLong()), info, chaptersElement)
        } else {
            concat(info, chaptersElement)
        }
        val ebmlHeader = element(0x1A45DFA3, element(0x4282, "matroska".toByteArray()))
        return concat(ebmlHeader, element(0x18538067, segmentBody))
    }

    // Vorbis comments (Ogg Vorbis I, RFC 7845, FLAC format)

    fun commentBlock(comments: List<String>): ByteArray =
        concat(lengthPrefixed("Abbay"), u32Le(comments.size), *comments.map(::lengthPrefixed).toTypedArray())

    private fun oggPage(sequence: Int, packet: ByteArray): ByteArray {
        val lacing = mutableListOf<Byte>()
        var remaining = packet.size
        while (remaining >= 255) {
            lacing += 255.toByte()
            remaining -= 255
        }
        lacing += remaining.toByte()
        val headerType = if (sequence == 0) 2 else 0
        return concat(
            "OggS".toByteArray(),
            byteArrayOf(0, headerType.toByte()),
            ByteArray(8),
            u32Le(1),
            u32Le(sequence),
            ByteArray(4),
            byteArrayOf(lacing.size.toByte()),
            lacing.toByteArray(),
            packet,
        )
    }

    fun oggOpus(comments: List<String>) = concat(
        oggPage(0, "OpusHead".toByteArray() + ByteArray(11)),
        oggPage(1, "OpusTags".toByteArray() + commentBlock(comments)),
    )

    fun oggVorbis(comments: List<String>) = concat(
        oggPage(0, byteArrayOf(1) + "vorbis".toByteArray() + ByteArray(23)),
        oggPage(1, byteArrayOf(3) + "vorbis".toByteArray() + commentBlock(comments) + byteArrayOf(1)),
    )

    fun flac(comments: List<String>): ByteArray {
        val vorbisComment = commentBlock(comments)
        val streamInfo = concat(byteArrayOf(0x00), u24(34), ByteArray(34))
        val lastBlockWithComments = concat(byteArrayOf((0x80 or 4).toByte()), u24(vorbisComment.size), vorbisComment)
        return concat("fLaC".toByteArray(), streamInfo, lastBlockWithComments)
    }
}
