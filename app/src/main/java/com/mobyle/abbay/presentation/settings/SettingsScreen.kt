package com.mobyle.abbay.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.booklist.BooksListViewModel
import com.mobyle.abbay.presentation.booklist.getBooks
import com.mobyle.abbay.presentation.booklist.getThumb
import com.mobyle.abbay.presentation.common.widgets.AbbayActionDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.model.BookType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    booksViewModel: BooksListViewModel,
    onStopPlayer: () -> Unit,
    close: () -> Unit
) {
    val shouldPlayWhenAppIsClosed by viewModel.shouldPlayWhenAppIsClosed.collectAsState()
    val shouldOpenPlayerInStartup by viewModel.shouldOpenPlayerInStartup.collectAsState()
    val showShowDeleteConfirmation by viewModel.showShowDeleteConfirmation.collectAsState()
    val showChangeFolderConfirmation by viewModel.showChangeFolderConfirmation.collectAsState()
    val asyncScope = rememberCoroutineScope()
    val context = LocalContext.current

    val openFolderSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            booksViewModel.showLoading()
            booksViewModel.saveBookFolderPath(it.toString())
            booksViewModel.selectBook(null, null)
            onStopPlayer()
            asyncScope.launch(Dispatchers.IO) {
                delay(500)
                it.getBooks(
                    context = context,
                    type = BookType.FOLDER
                )?.let {
                    val booksWithThumbnails = it.mapNotNull { book ->
                        book.getThumb(context)
                    }

                    booksViewModel.addBooksFromNewFolder(booksWithThumbnails)
                }
            }
        }
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

                SettingItem(
                    text = stringResource(R.string.change_folder),
                    onClick = {
                        viewModel.showChangeFolderConfirmation()
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
                    onStopPlayer()
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