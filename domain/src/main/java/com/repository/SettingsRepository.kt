package com.repository

interface SettingsRepository {
    fun isPlayWhenAppIsClosedEnabled(): Boolean

    fun isOpenPlayerInStartup(): Boolean

    fun enablePlayWhenAppIsClosed()

    fun disablePlayWhenAppIsClosed()

    fun enableOpenPlayerInStartup()

    fun disableOpenPlayerInStartup()
}