package com.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun isPlayWhenAppIsClosedEnabled(): Boolean

    fun isOpenPlayerInStartup(): Boolean

    fun enablePlayWhenAppIsClosed()

    fun disablePlayWhenAppIsClosed()

    fun enableOpenPlayerInStartup()

    fun disableOpenPlayerInStartup()
}