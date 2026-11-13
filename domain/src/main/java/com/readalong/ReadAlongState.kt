package com.readalong

/**
 * The languages the on-device recogniser can be given. Each one is a speech model the user installs once,
 * on the device, only when the text is first shown for a book in it.
 */
enum class ReadAlongLanguage(val code: String) {
    ENGLISH("en"),
    SPANISH("es"),
    PORTUGUESE("pt");

    companion object {
        /** The language for a two-letter code, or the closest one the recogniser has. */
        fun ofCode(code: String?): ReadAlongLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

/** Whether a language's speech model can be used now, after a download, or not at all. */
enum class ReadAlongModelStatus { INSTALLED, DOWNLOADABLE, UNSUPPORTED }

/**
 * What the text pane says while there is no line to show.
 */
sealed interface ReadAlongState {
    /** Detecting the language, or no line yet at the shown time. */
    data object Preparing : ReadAlongState

    /** Downloading the speech model for this language. */
    data class DownloadingModel(val language: ReadAlongLanguage) : ReadAlongState

    /** No speech model exists for this language. */
    data class Unsupported(val language: ReadAlongLanguage) : ReadAlongState

    /** The chapter at the shown time isn't on the device. */
    data object NeedsDownload : ReadAlongState

    /** The book's file isn't on the device. */
    data object FileMissing : ReadAlongState

    data object Failed : ReadAlongState

    /** The shown time is covered: by a line, or by silence. */
    data object Ready : ReadAlongState
}

/** Where the reader is in the text, as the pane shows it. */
data class ReadAlongReading(val display: ReadAlongDisplay, val lines: List<ReadAlongLine>)
