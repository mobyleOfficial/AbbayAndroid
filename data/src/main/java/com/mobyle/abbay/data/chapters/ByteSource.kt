package com.mobyle.abbay.data.chapters

/** Positional read access to a file, so parsers jump between boxes without loading whole files. */
interface ByteSource {
    val size: Long

    /** Up to [length] bytes at [position]; fewer at the end of the file. */
    fun read(position: Long, length: Int): ByteArray
}

class ByteArraySource(private val bytes: ByteArray) : ByteSource {
    override val size: Long = bytes.size.toLong()

    override fun read(position: Long, length: Int): ByteArray {
        if (position >= bytes.size || position < 0) return ByteArray(0)
        val end = minOf(bytes.size.toLong(), position + length).toInt()
        return bytes.copyOfRange(position.toInt(), end)
    }
}
