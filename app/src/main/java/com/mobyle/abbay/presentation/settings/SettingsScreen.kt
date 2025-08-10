package com.mobyle.abbay.presentation.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayActionDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    close: () -> Unit
) {
    val shouldPlayWhenAppIsClosed by viewModel.shouldPlayWhenAppIsClosed.collectAsState()
    val shouldOpenPlayerInStartup by viewModel.shouldOpenPlayerInStartup.collectAsState()
    val showShowDeleteConfirmation by viewModel.showShowDeleteConfirmation.collectAsState()

    AbbayScreen(
        title = stringResource(R.string.settings),
        onClose = close
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            item {
                SettingItem(
                    text = stringResource(R.string.play_when_app_closed),
                    content = {
                        Switch(
                            checked = shouldPlayWhenAppIsClosed,
                            onCheckedChange = viewModel::changePlayWhenAppIsClosed
                        )
                    }
                )
                SettingItem(
                    text = stringResource(R.string.open_player_in_startup),
                    content = {
                        Switch(
                            checked = shouldOpenPlayerInStartup,
                            onCheckedChange = viewModel::changeOpenPlayerInStartUp
                        )
                    }
                )

                SettingItem(
                    text = stringResource(R.string.delete_all_books),
                    onClick = viewModel::showDeleteConfirmation
                )
            }
        }

        if (showShowDeleteConfirmation) {
            AbbayActionDialog(
                onDismiss = viewModel::dismissDeleteConfirmation,
                title = stringResource(R.string.delete_all_books_dialog_title),
                body = stringResource(R.string.delete_all_books_dialog_body),
                actionButtonTitle = stringResource(R.string.delete),
                onAction = {
                    viewModel.clearBooks()
                    viewModel.dismissDeleteConfirmation()
                },
            )
        }
    }
}