package com.mobyle.abbay.presentation.cast

import com.mobyle.abbay.presentation.playback.remote.ProcessedAudioStream
import java.io.InputStream

/** The processed audio of Cast phase 2 as the server's live source. */
fun ProcessedAudioStream.asLiveSource(): LocalMediaServer.LiveSource = object : LocalMediaServer.LiveSource {
    override val mimeType: String = ProcessedAudioStream.MIME_TYPE

    override fun header(generation: Int): ByteArray? = this@asLiveSource.header(generation)

    override fun open(generation: Int): InputStream? = this@asLiveSource.open(generation)
}
