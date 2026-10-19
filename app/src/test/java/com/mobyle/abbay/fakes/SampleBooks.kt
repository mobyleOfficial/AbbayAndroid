package com.mobyle.abbay.fakes

import com.model.BookFile
import com.model.BookType
import com.model.Chapter
import com.model.MultipleBooks

object SampleBooks {
    val singleFileBook = BookFile(
        id = "101",
        name = "The Long Road Home",
        thumbnail = null,
        progress = 754_000L,
        duration = 36_000_000L,
        speed = 1f,
        type = BookType.FILE,
        fileName = "long_road_home.m4b",
        chapters = listOf(
            Chapter(0, "Departure", 0L, 600_000L),
            Chapter(1, "The Crossing", 600_000L, 7_200_000L),
            Chapter(2, "Home", 7_200_000L, 36_000_000L),
        ),
    )

    private val chapterFiles = (1..3).map { chapterNumber ->
        BookFile(
            id = "20$chapterNumber",
            name = "Stories of the Sea",
            thumbnail = null,
            progress = 0L,
            duration = 1_800_000L,
            speed = 1f,
            type = BookType.FOLDER,
            fileName = "chapter_0$chapterNumber.mp3",
        )
    }

    val multipleFilesBook = MultipleBooks(
        id = "201",
        name = "Stories of the Sea",
        thumbnail = null,
        progress = 120_000L,
        duration = chapterFiles.sumOf { it.duration },
        speed = 1.5f,
        type = BookType.FOLDER,
        bookFileList = chapterFiles,
        currentBookPosition = 1,
        chapters = chapterFiles.mapIndexed { index, file ->
            Chapter(index, "", index * file.duration, (index + 1) * file.duration)
        },
    )

    val missingFileBook = singleFileBook.copy(id = "102", name = "A Book That Was Moved", hasError = true)
}
