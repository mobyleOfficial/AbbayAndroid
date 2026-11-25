package com.mobyle.abbay.presentation.widget

import com.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Widgets are not created by Hilt, so they reach the app settings through this entry point. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun settingsRepository(): SettingsRepository
}
