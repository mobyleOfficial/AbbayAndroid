package com.mobyle.abbay.presentation.playback.remote

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ProcessedAudioStream(private val capacityBytes: Int = DEFAULT_CAPACITY_BYTES) {
    interface Listener {
        fun onPlay()

        fun onPause()

        fun onNewGeneration(generation: Int)
    }

    @Volatile
    var listener: Listener? = null

    private val lock = ReentrantLock()
    private val dataAvailable = lock.newCondition()
    private val buffer = ByteArray(capacityBytes)
    private var readPosition = 0L
    private var writePosition = 0L
    private var isClosed = false

    @Volatile
    var generation = 0
        private set

    @Volatile
    var sampleRateHz = 0
        private set

    @Volatile
    var channelCount = 0
        private set

    @Volatile
    private var receiver = ReceiverPosition(generation = -1, positionMs = 0L, isPlaying = false, atNanos = 0L)

    fun beginGeneration(sampleRateHz: Int, channelCount: Int): Int {
        val next = lock.withLock {
            generation += 1
            this.sampleRateHz = sampleRateHz
            this.channelCount = channelCount
            readPosition = 0L
            writePosition = 0L
            dataAvailable.signalAll()
            generation
        }
        listener?.onNewGeneration(next)
        return next
    }

    fun write(data: ByteBuffer): Int = lock.withLock {
        val frameBytes = (channelCount * BYTES_PER_SAMPLE).coerceAtLeast(1)
        val free = capacityBytes - (writePosition - readPosition).toInt()
        val count = (minOf(free, data.remaining()) / frameBytes) * frameBytes
        repeat(count) { offset -> buffer[((writePosition + offset) % capacityBytes).toInt()] = data.get() }
        writePosition += count
        if (count > 0) dataAvailable.signalAll()
        count
    }

    val bufferedBytes: Int get() = lock.withLock { (writePosition - readPosition).toInt() }

    fun header(generation: Int): ByteArray? = if (generation != this.generation ||
        sampleRateHz == 0
    ) {
        null
    } else {
        wavHeader(sampleRateHz, channelCount)
    }

    fun open(generation: Int): InputStream? = if (generation != this.generation) null else Reader(generation)

    fun play() = listener?.onPlay()

    fun pause() = listener?.onPause()

    fun updateReceiver(generation: Int, positionMs: Long, isPlaying: Boolean, atNanos: Long = System.nanoTime()) {
        receiver = ReceiverPosition(generation, positionMs, isPlaying, atNanos)
    }

    fun playedFrames(nowNanos: Long = System.nanoTime()): Long? {
        val current = receiver
        if (current.generation != generation || sampleRateHz == 0) return null
        val elapsedMs = if (current.isPlaying) TimeUnit.NANOSECONDS.toMillis(nowNanos - current.atNanos).coerceAtLeast(0L) else 0L
        return (current.positionMs + elapsedMs) * sampleRateHz / MILLIS_PER_SECOND
    }

    fun close() = lock.withLock {
        isClosed = true
        dataAvailable.signalAll()
    }

    private inner class Reader(private val readerGeneration: Int) : InputStream() {
        override fun read(): Int {
            val single = ByteArray(1)
            return if (read(single, 0, 1) < 0) -1 else single[0].toInt() and BYTE_MASK
        }

        override fun read(target: ByteArray, offset: Int, length: Int): Int = lock.withLock {
            if (length == 0) return 0
            while (!isClosed && readerGeneration == generation && writePosition == readPosition) {
                dataAvailable.await(READ_WAIT_MS, TimeUnit.MILLISECONDS)
            }
            if (isClosed || readerGeneration != generation) return -1
            val count = minOf(length, (writePosition - readPosition).toInt())
            repeat(count) { index -> target[offset + index] = buffer[((readPosition + index) % capacityBytes).toInt()] }
            readPosition += count
            count
        }
    }

    private class ReceiverPosition(val generation: Int, val positionMs: Long, val isPlaying: Boolean, val atNanos: Long)

    companion object {
        const val DEFAULT_CAPACITY_BYTES = 512 * 1024
        const val BYTES_PER_SAMPLE = 2
        const val MIME_TYPE = "audio/wav"
        private const val MILLIS_PER_SECOND = 1_000L
        private const val READ_WAIT_MS = 500L
        private const val BYTE_MASK = 0xFF
        private const val HEADER_BYTES = 44
        private const val FORMAT_CHUNK_BYTES = 16
        private const val FORMAT_PCM: Short = 1
        private const val BITS_PER_SAMPLE: Short = 16

        private const val UNKNOWN_SIZE = -1

        fun wavHeader(sampleRateHz: Int, channelCount: Int): ByteArray =
            ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(UNKNOWN_SIZE)
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(FORMAT_CHUNK_BYTES)
                putShort(FORMAT_PCM)
                putShort(channelCount.toShort())
                putInt(sampleRateHz)
                putInt(sampleRateHz * channelCount * BYTES_PER_SAMPLE)
                putShort((channelCount * BYTES_PER_SAMPLE).toShort())
                putShort(BITS_PER_SAMPLE)
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(UNKNOWN_SIZE)
            }.array()
    }
}
