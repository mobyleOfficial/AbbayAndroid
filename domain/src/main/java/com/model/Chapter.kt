package com.model

/** A chapter with book-absolute times. A blank title is shown as "Chapter n". */
data class Chapter(val index: Int, val title: String, val startMs: Long, val endMs: Long)
