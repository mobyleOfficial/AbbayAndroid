package com.mobyle.abbay.data.repository

import com.mobyle.abbay.data.datasource.local.keystore.KeyValueStore
import com.mobyle.abbay.data.datasource.local.keystore.KeyValueStoreKeys
import com.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val keyValueStore: KeyValueStore
) : SettingsRepository {

    override fun isPlayWhenAppIsClosedEnabled(): Boolean {
        val key = KeyValueStoreKeys.PLAY_WHEN_APP_IS_CLOSED

        return keyValueStore.getBooleanStoredValue(key)
    }

    override fun isOpenPlayerInStartup(): Boolean {
        val key = KeyValueStoreKeys.OPEN_PLAYER_IN_STARTUP

        return keyValueStore.getBooleanStoredValue(key)
    }

    override fun enablePlayWhenAppIsClosed() {
        val key = KeyValueStoreKeys.PLAY_WHEN_APP_IS_CLOSED

        keyValueStore.storeBooleanValue(key, true)
    }

    override fun disablePlayWhenAppIsClosed() {
        val key = KeyValueStoreKeys.PLAY_WHEN_APP_IS_CLOSED

        keyValueStore.storeBooleanValue(key, false)
    }

    override fun enableOpenPlayerInStartup() {
        val key = KeyValueStoreKeys.OPEN_PLAYER_IN_STARTUP

        keyValueStore.storeBooleanValue(key, true)
    }

    override fun disableOpenPlayerInStartup() {
        val key = KeyValueStoreKeys.OPEN_PLAYER_IN_STARTUP

        keyValueStore.storeBooleanValue(key, false)
    }
}