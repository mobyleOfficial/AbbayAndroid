package com.mobyle.abbay.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen

@Composable
fun SettingsScreen() {
    AbbayScreen(
        title = "Settings"
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            item {
                SettingItem(
                    text = "Play when app is closed",
                    onClick = {}
                )
                SettingItem(
                    text = "Open player in startup",
                    onClick = {}
                )
                SettingItem(
                    text = "Select App color",
                    onClick = {}
                )
                SettingItem(
                    text = "Delete all books",
                    onClick = {}
                )
            }
        }
    }
}