package com.model

/**
 * Where a folder book's title comes from. Books that sit directly in a library folder, or were added as single
 * files, always take their title from the file, whatever is chosen here.
 */
enum class BookNameSource {
    /** Follow the app-wide setting. */
    APP_DEFAULT,

    /** The name of the folder that holds the book's files. */
    FOLDER,

    /** The album or title tag of the audio files, falling back to the folder name when untagged. */
    METADATA,
}
