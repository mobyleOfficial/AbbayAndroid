package com.repository

import com.player.PlaybackOptions
import kotlinx.coroutines.flow.Flow

interface PlaybackOptionsRepository {
    fun observeOptions(bookId: String): Flow<PlaybackOptions>

    suspend fun updateOptions(bookId: String, options: PlaybackOptions)
}
