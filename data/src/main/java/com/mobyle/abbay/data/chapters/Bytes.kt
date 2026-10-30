package com.mobyle.abbay.data.chapters

/** Big- and little-endian helpers shared by the parsers. */
internal object Bytes {
    fun u8(data: ByteArray, offset: Int): Int = data[offset].toInt() and 0xFF

    fun u16(data: ByteArray, offset: Int): Int = (u8(data, offset) shl 8) or u8(data, offset + 1)

    fun u24(data: ByteArray, offset: Int): Int = (u16(data, offset) shl 8) or u8(data, offset + 2)

    fun u32(data: ByteArray, offset: Int): Long = (u16(data, offset).toLong() shl 16) or u16(data, offset + 2).toLong()

    fun u64(data: ByteArray, offset: Int): Long = (u32(data, offset) shl 32) or u32(data, offset + 4)

    fun u32Le(data: ByteArray, offset: Int): Long =
        u8(data, offset).toLong() or (u8(data, offset + 1).toLong() shl 8) or
            (u8(data, offset + 2).toLong() shl 16) or (u8(data, offset + 3).toLong() shl 24)

    fun ascii(data: ByteArray, offset: Int, length: Int): String = String(data, offset, length, Charsets.US_ASCII)

    fun startsWith(data: ByteArray, prefix: String, offset: Int = 0): Boolean =
        data.size >= offset + prefix.length && ascii(data, offset, prefix.length) == prefix
}
