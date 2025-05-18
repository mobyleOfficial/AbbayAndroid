package com.mobyle.abbay.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    close: () -> Unit
) {
    val shouldPlayWhenAppIsClosed by viewModel.shouldPlayWhenAppIsClosed.collectAsState()
    val shouldOpenPlayerInStartup by viewModel.shouldOpenPlayerInStartup.collectAsState()

    AbbayScreen(
        title = "Settings",
        onClose = close
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            item {
                SettingItem(
                    text = "Play when app is closed",
                    content = {
                        Switch(
                            checked = shouldPlayWhenAppIsClosed,
                            onCheckedChange = viewModel::changePlayWhenAppIsClosed
                        )
                    }
                )
                SettingItem(
                    text = "Open player in startup",
                    content = {
                        Switch(
                            checked = shouldOpenPlayerInStartup,
                            onCheckedChange = viewModel::changeOpenPlayerInStartUp
                        )
                    }
                )

                SettingItem(
                    text = "Delete all books",
                    onClick = viewModel::clearBooks
                )
            }
        }
    }
}