package com.mobyle.abbay.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.BuildConfig
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.BooksListViewModel
import com.mobyle.abbay.presentation.common.widgets.AbbayActionDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    booksViewModel: BooksListViewModel,
    onStopPlayer: () -> Unit,
    openDiagnostics: () -> Unit = {},
    openLicenses: () -> Unit = {},
    openConverter: (() -> Unit)? = null,
    close: () -> Unit
) {
    val shouldPlayWhenAppIsClosed by viewModel.shouldPlayWhenAppIsClosed.collectAsState()
    val shouldOpenPlayerInStartup by viewModel.shouldOpenPlayerInStartup.collectAsState()
    val showShowDeleteConfirmation by viewModel.showShowDeleteConfirmation.collectAsState()
    val showChangeFolderConfirmation by viewModel.showChangeFolderConfirmation.collectAsState()

    val libraryRoots by viewModel.libraryRoots.collectAsState()
    val settings by viewModel.listeningSettings.collectAsState()

    val openFolderSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            booksViewModel.selectBook(null, null)
            onStopPlayer()
            viewModel.changeFolder(it.toString())
        }
    }

    val addFolderSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.addFolder(it.toString()) }
    }

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
                    info = stringResource(R.string.settings_info_play_when_closed),
                    content = {
                        Switch(
                            checked = shouldPlayWhenAppIsClosed,
                            onCheckedChange = viewModel::changePlayWhenAppIsClosed,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                                checkedTrackColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                )
                SettingItem(
                    text = stringResource(R.string.open_player_in_startup),
                    info = stringResource(R.string.settings_info_open_player),
                    content = {
                        Switch(
                            checked = shouldOpenPlayerInStartup,
                            onCheckedChange = viewModel::changeOpenPlayerInStartUp,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                                checkedTrackColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                )

                SectionHeader(R.string.settings_library)
                SettingItem(
                    text = stringResource(R.string.change_folder),
                    info = stringResource(R.string.settings_info_change_folder),
                    onClick = {
                        viewModel.showChangeFolderConfirmation()
                    }
                )

                SettingItem(
                    text = stringResource(R.string.delete_all_books),
                    info = stringResource(R.string.settings_info_delete_all),
                    onClick = viewModel::showDeleteConfirmation
                )
            }

            if (libraryRoots.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.settings_library_folders),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }

                items(items = libraryRoots, key = { root -> root.id }) { root ->
                    LibraryFolderItem(
                        root = root,
                        canRemove = libraryRoots.size > 1,
                        onRemove = { viewModel.removeFolder(root.id) }
                    )
                }

                item {
                    SettingItem(
                        text = stringResource(R.string.settings_add_library_folder),
                        onClick = { addFolderSelector.launch(null) }
                    )
                }

                openConverter?.let { open -> item {
                        SettingItem(
                            text = stringResource(R.string.converter_title),
                            onClick = open,
                            info = stringResource(R.string.settings_info_converter),
                        )
                    } }
            }

            item { LibraryPreferenceSettings(settings, viewModel::changeListeningSettings) }

            item { BackupSettings(viewModel) }

            item { ListeningSettings(viewModel) }

            item { AppearanceSettings(viewModel) }

            item { ExperimentalSettings(viewModel) }

            item {
                AboutSettings(
                    viewModel = viewModel,
                    openDiagnostics = openDiagnostics,
                    openLicenses = openLicenses,
                    replayWalkthrough = {
                        viewModel.replayWalkthrough()
                        close()
                    },
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
                    onStopPlayer()
                    booksViewModel.selectBook(null, null)
                    viewModel.clearBooks()
                    viewModel.dismissDeleteConfirmation()
                },
            )
        }

        if (showChangeFolderConfirmation) {
            AbbayActionDialog(
                onDismiss = viewModel::dismissChangeFolderConfirmation,
                title = stringResource(R.string.change_folder_dialog_title),
                body = stringResource(R.string.change_folder_dialog_body),
                actionButtonTitle = stringResource(R.string.change),
                onAction = {
                    viewModel.dismissChangeFolderConfirmation()
                    openFolderSelector.launch(null)
                },
            )
        }
    }
}

private fun openUrl(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}

/** Straight to the Play Store app's page for Abbay, or the store's web page on a device without it. */
private fun openStoreListing(context: Context) {
    val packageName = context.packageName
    if (!openUrl(context, "market://details?id=$packageName")) {
        openUrl(context, playStoreUrl(packageName))
    }
}

private fun shareApp(context: Context) {
    val message = context.getString(R.string.settings_share_app_message, playStoreUrl(context.packageName))
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
        .putExtra(Intent.EXTRA_TEXT, message)
    try {
        context.startActivity(Intent.createChooser(send, context.getString(R.string.settings_share_app)))
    } catch (_: ActivityNotFoundException) {
    }
}

private fun playStoreUrl(packageName: String) = "https://play.google.com/store/apps/details?id=$packageName"

@Composable
private fun AboutSettings(
    viewModel: SettingsViewModel,
    openDiagnostics: () -> Unit,
    openLicenses: () -> Unit,
    replayWalkthrough: () -> Unit,
) {
    val context = LocalContext.current
    val sourceCodeUrl = stringResource(R.string.source_code_url)
    Column {
        SectionHeader(R.string.settings_about)
        SettingItem(text = stringResource(R.string.settings_rate_app), onClick = { openStoreListing(context) })
        SettingItem(text = stringResource(R.string.settings_share_app), onClick = { shareApp(context) })
        SettingItem(text = stringResource(R.string.settings_walkthrough), onClick = replayWalkthrough)
        SettingItem(text = stringResource(R.string.diagnostics_title), onClick = openDiagnostics)
        SettingItem(text = stringResource(R.string.settings_licenses), onClick = openLicenses)
        SettingItem(text = stringResource(R.string.settings_source_code), onClick = { openUrl(context, sourceCodeUrl) })
        SectionHeader(R.string.settings_privacy)
        PrivacySettings(viewModel)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.app_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
