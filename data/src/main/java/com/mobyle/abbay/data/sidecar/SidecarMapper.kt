package com.mobyle.abbay.data.sidecar

import com.mobyle.abbay.data.database.dao.BookWithDetails
import com.mobyle.abbay.data.database.entity.BookmarkEntity
import com.mobyle.abbay.data.database.entity.CharacterNoteEntity
import com.mobyle.abbay.data.database.entity.ListeningStateEntity
import com.mobyle.abbay.data.library.BookProjection

internal object SidecarMapper {
    fun toSidecar(details: BookWithDetails, bookmarks: List<BookmarkEntity>, characters: List<CharacterNoteEntity>): SidecarBook {
        val book = details.book
        val state = details.listeningState ?: ListeningStateEntity(bookId = book.id)
        val track = BookProjection.playableTracks(details).getOrNull(state.trackIndex)
        return SidecarBook(
            id = book.sidecarId,
            updatedAt = state.updatedAt,
            position = track?.let { SidecarBook.Position(it.fileName, state.positionInTrackMs, state.bookPositionMs) },
            state = state.state,
            lastPlayedAt = state.lastPlayedAt,
            lastPausedAt = state.lastPausedAt,
            audio = SidecarBook.Audio(
                speed = state.speed,
                pitchSemitones = state.pitchSemitones,
                silenceTrim = state.silenceTrimLevel,
                boost = state.boostLevel,
                mono = state.mono,
                balance = state.balance,
                noiseReduction = state.noiseReductionLevel,
                equalizerJson = state.equalizerJson,
            ),
            repeatMode = state.repeatMode,
            loopAMs = state.loopAMs,
            loopBMs = state.loopBMs,
            skipIntroSeconds = state.skipIntroSeconds,
            skipOutroSeconds = state.skipOutroSeconds,
            sortByTrackTag = state.sortByTrackTag,
            bookmarks = bookmarks.map { SidecarBook.Bookmark(it.bookPositionMs, it.chapterTitle, it.note, it.createdAt) },
            characters = characters.map { SidecarBook.Character(it.name, it.description, it.firstPositionMs) },
            overrides = SidecarBook.Overrides(book.titleOverride, book.authorOverride, book.narratorOverride, book.descriptionOverride),
        )
    }

    fun toListeningState(sidecar: SidecarBook, details: BookWithDetails): ListeningStateEntity {
        val current = details.listeningState ?: ListeningStateEntity(bookId = details.book.id)
        val ordered = BookProjection.playableTracks(
            details.copy(listeningState = current.copy(sortByTrackTag = sidecar.sortByTrackTag)),
        )
        val trackIndex = sidecar.position?.let { position -> ordered.indexOfFirst { it.fileName == position.trackFileName } } ?: -1
        return current.copy(
            trackIndex = trackIndex.coerceAtLeast(0),
            positionInTrackMs = if (trackIndex >= 0) sidecar.position?.positionInTrackMs ?: 0L else 0L,
            bookPositionMs = sidecar.position?.bookPositionMs ?: 0L,
            state = sidecar.state,
            lastPlayedAt = sidecar.lastPlayedAt,
            lastPausedAt = sidecar.lastPausedAt,
            speed = sidecar.audio.speed,
            pitchSemitones = sidecar.audio.pitchSemitones,
            silenceTrimLevel = sidecar.audio.silenceTrim,
            boostLevel = sidecar.audio.boost,
            mono = sidecar.audio.mono,
            balance = sidecar.audio.balance,
            noiseReductionLevel = sidecar.audio.noiseReduction,
            equalizerJson = sidecar.audio.equalizerJson,
            repeatMode = sidecar.repeatMode,
            loopAMs = sidecar.loopAMs,
            loopBMs = sidecar.loopBMs,
            skipIntroSeconds = sidecar.skipIntroSeconds,
            skipOutroSeconds = sidecar.skipOutroSeconds,
            sortByTrackTag = sidecar.sortByTrackTag,
            updatedAt = sidecar.updatedAt,
        )
    }

    fun toBookmarks(sidecar: SidecarBook, bookId: Long): List<BookmarkEntity> = sidecar.bookmarks.map {
        BookmarkEntity(
            bookId = bookId,
            bookPositionMs = it.bookPositionMs,
            chapterTitle = it.chapterTitle,
            note = it.note,
            createdAt = it.createdAt
        )
    }

    fun toCharacters(sidecar: SidecarBook, bookId: Long): List<CharacterNoteEntity> = sidecar.characters.map {
        CharacterNoteEntity(bookId = bookId, name = it.name, description = it.description, firstPositionMs = it.firstPositionMs)
    }

    fun isNewer(sidecar: SidecarBook, details: BookWithDetails): Boolean =
        details.listeningState?.let { sidecar.updatedAt > it.updatedAt } ?: true
}
