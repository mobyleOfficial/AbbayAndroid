package com.mobyle.abbay.data.scanner

import com.mobyle.abbay.data.storage.DocumentEntry

/** File classification rules. */
@Suppress("TooManyFunctions") // One predicate per file kind.
internal object FileKinds {
    private val audioExtensions = setOf(
        "mp3", "m4a", "m4b", "mp4", "aac", "ogg", "oga", "opus", "flac", "wav", "aiff", "aif",
        "mka", "webm", "weba", "3gp", "amr", "mp2", "mpga",
    )

    /** Detected but never playable. */
    private val unsupportedAudioExtensions = setOf("wma")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val documentExtensions = setOf("pdf", "epub", "txt", "fb2")
    private val preferredCoverNames = setOf("cover", "folder", "front")
    private val descriptionFileNames = setOf("description.txt", "info.txt")
    private val discFolderPattern = Regex("^(cd|disc|disk|part)\\s*\\d+$", RegexOption.IGNORE_CASE)

    /** Folder of Abbay's own files inside a book folder. */
    const val SIDECAR_FOLDER = ".abbay"

    fun isIgnored(entry: DocumentEntry): Boolean = entry.name.startsWith(".") || (!entry.isDirectory && entry.sizeBytes == 0L)

    fun isAudio(entry: DocumentEntry): Boolean = !entry.isDirectory && extensionOf(entry.name) in audioExtensions

    fun isUnsupportedAudio(entry: DocumentEntry): Boolean = !entry.isDirectory && extensionOf(entry.name) in unsupportedAudioExtensions

    fun isImage(entry: DocumentEntry): Boolean = !entry.isDirectory && extensionOf(entry.name) in imageExtensions

    fun isCueSheet(entry: DocumentEntry): Boolean = !entry.isDirectory && extensionOf(entry.name) == "cue"

    /** A companion document; description files are not. */
    fun isCompanionDocument(entry: DocumentEntry): Boolean =
        !entry.isDirectory && extensionOf(entry.name) in documentExtensions && !isDescription(entry)

    fun isDescription(entry: DocumentEntry): Boolean = !entry.isDirectory && entry.name.lowercase() in descriptionFileNames

    fun isDiscFolder(entry: DocumentEntry): Boolean = entry.isDirectory && discFolderPattern.matches(entry.name.trim())

    /** Cover/folder/front image first, otherwise the largest image. */
    fun preferredCover(images: List<DocumentEntry>): DocumentEntry? =
        images.firstOrNull { baseName(it.name).lowercase() in preferredCoverNames } ?: images.maxByOrNull { it.sizeBytes }

    fun baseName(fileName: String): String = fileName.substringBeforeLast('.', fileName)

    private fun extensionOf(fileName: String): String = fileName.substringAfterLast('.', "").lowercase()
}
