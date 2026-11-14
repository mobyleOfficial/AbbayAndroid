package com.mobyle.abbay.data.librivox

data class ArchiveSearchDoc(
    val identifier: String?,
    val title: String?,
    val creators: List<String>,
    val languages: List<String>,
    val mediatype: String?,
)

data class ArchiveItem(
    val identifier: String,
    val title: String?,
    val creators: List<String>,
    val languages: List<String>,
    val description: String?,
    val mediatype: String?,
    val runtime: String?,
    val files: List<ArchiveFile>,
) {
    val creator: String? get() = creators.firstOrNull()
}

data class ArchiveFile(
    val name: String?,
    val format: String?,
    val size: String?,
    val length: String?,
    val title: String?,
    val track: String?,
)

data class LibriVoxFeedBook(
    val id: String?,
    val urlIArchive: String?,
    val description: String?,
    val sections: List<LibriVoxFeedSection>,
) {
    val archiveIdentifier: String?
        get() = urlIArchive?.trimEnd('/')?.substringAfterLast('/')?.takeIf(String::isNotEmpty)
}

data class LibriVoxFeedSection(val title: String?, val listenUrl: String?, val playtime: String?)
