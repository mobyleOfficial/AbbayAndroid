package com.mobyle.abbay.presentation.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mobyle.abbay.R
import java.time.LocalDate

/** "Save progress in book folders" and Backup settings. */
@Composable
fun BackupSettings(viewModel: SettingsViewModel) {
    val settings by viewModel.listeningSettings.collectAsState()
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON_MIME_TYPE)) { uri ->
        uri?.let { viewModel.exportBackupTo(it.toString()) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackupFrom(it.toString()) }
    }

    val resources = context.resources
    LaunchedEffect(Unit) {
        viewModel.backupResults.collect { result ->
            val message = when (result) {
                is BackupResult.Exported -> resources.getQuantityString(R.plurals.backup_exported, result.books, result.books)
                is BackupResult.Imported -> resources.getQuantityString(R.plurals.backup_imported, result.books, result.books) +
                    if (result.unmatchedBooks > 0) {
                        " " + resources.getQuantityString(R.plurals.backup_unmatched, result.unmatchedBooks, result.unmatchedBooks)
                    } else {
                        ""
                    }

                BackupResult.NotABackup -> context.getString(R.string.backup_not_a_backup)
                BackupResult.Failed -> context.getString(R.string.backup_failed)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Column {
        SettingItem(text = stringResource(R.string.settings_save_progress_in_folders)) {
            SettingSwitch(settings.saveProgressInFolders, onCheckedChange = { enabled ->
                viewModel.changeListeningSettings { it.copy(saveProgressInFolders = enabled) }
            })
        }
        SectionHeader(R.string.settings_backup)
        SettingItem(text = stringResource(R.string.settings_export_backup), onClick = { exporter.launch(backupFileName()) })
        SettingItem(text = stringResource(R.string.settings_import_backup), onClick = {
            importer.launch(arrayOf(JSON_MIME_TYPE, ANY_MIME_TYPE))
        })
    }
}

private fun backupFileName(): String = "abbay-backup-${LocalDate.now()}.json"

private const val JSON_MIME_TYPE = "application/json"
private const val ANY_MIME_TYPE = "*/*"
