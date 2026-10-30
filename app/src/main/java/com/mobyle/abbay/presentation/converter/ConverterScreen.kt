package com.mobyle.abbay.presentation.converter

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.converter.ConversionEnd
import com.converter.ConversionProgress
import com.converter.ConversionScope
import com.converter.ConverterOptions
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.bookmarks.EmptyMessage
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.settings.SettingSwitch

@Composable
fun ConverterScreen(viewModel: ConverterViewModel = hiltViewModel(), close: () -> Unit) {
    AbbayScreen(title = stringResource(R.string.converter_title), onClose = close) {
        if (viewModel.encoderName == null) {
            EmptyMessage(stringResource(R.string.diagnostics_opus_missing))
            return@AbbayScreen
        }
        val progress by viewModel.progress.collectAsState()
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text(stringResource(R.string.converter_explanation), color = AbbayColors.content, style = MaterialTheme.typography.bodyMedium)
            if (progress.isRunning) Running(progress, viewModel::cancel) else Setup(viewModel, progress)
            val totalSaved by viewModel.totalSaved.collectAsState()
            if (totalSaved > 0) {
                Text(
                    stringResource(R.string.converter_total_saved, Formatter.formatShortFileSize(LocalContext.current, totalSaved)),
                    color = AbbayColors.content,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun Setup(viewModel: ConverterViewModel, lastRun: ConversionProgress) {
    val scope by viewModel.scope.collectAsState()
    val plan by viewModel.plan.collectAsState()
    val context = LocalContext.current
    var isConfirmingDelete by remember { mutableStateOf(false) }

    lastRun.end?.let { end -> Text(endMessage(end, lastRun), color = AbbayColors.content) }
    if (!viewModel.isBookScope) RootChoice(viewModel, scope)
    OptionSections(viewModel, onAskDelete = { isConfirmingDelete = true })
    plan?.let { current ->
        Text(
            if (current.fileCount == 0) {
                stringResource(R.string.converter_nothing)
            } else {
                stringResource(
                    R.string.converter_plan,
                    current.fileCount,
                    Formatter.formatShortFileSize(context, current.totalBytes),
                    Formatter.formatShortFileSize(context, current.estimatedSavingBytes),
                )
            },
            color = AbbayColors.content,
        )
    }
    Button(
        onClick = viewModel::start,
        enabled = scope != null && (plan?.fileCount ?: 0) > 0,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.converter_start))
    }
    if (isConfirmingDelete) {
        AbbayAlertDialog(
            onDismissRequest = { isConfirmingDelete = false },
            title = { Text(stringResource(R.string.converter_originals_delete)) },
            text = { Text(stringResource(R.string.converter_delete_confirm)) },
            confirmButton = {
                AbbayTextButton(onClick = {
                    isConfirmingDelete = false
                    viewModel.changeOptions { it.copy(deleteOriginals = true) }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { AbbayTextButton(onClick = { isConfirmingDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun OptionSections(viewModel: ConverterViewModel, onAskDelete: () -> Unit) {
    val options by viewModel.options.collectAsState()
    val settings by viewModel.settings.collectAsState()
    Section(stringResource(R.string.converter_bitrate)) {
        ChipRow(ConverterOptions.BITRATES, options.bitrateKbps, { stringResource(R.string.converter_kbps, it) }) { bitrate ->
            viewModel.changeOptions { it.copy(bitrateKbps = bitrate) }
        }
    }
    SwitchRow(stringResource(R.string.converter_mono), options.forceMono) { mono -> viewModel.changeOptions { it.copy(forceMono = mono) } }
    SwitchRow(stringResource(R.string.converter_copy_cover), settings.converterCopyCover) { copy ->
        viewModel.changeSettings { it.copy(converterCopyCover = copy) }
    }
    Section(stringResource(R.string.converter_originals)) {
        Column(Modifier.selectableGroup()) {
            RadioRow(stringResource(R.string.converter_originals_move), !options.deleteOriginals) {
                viewModel.changeOptions { it.copy(deleteOriginals = false) }
            }
            RadioRow(stringResource(R.string.converter_originals_delete), options.deleteOriginals) { onAskDelete() }
        }
    }
    Section(stringResource(R.string.converter_battery)) {
        ChipRow(ConverterViewModel.BATTERY_MINIMUMS, settings.converterMinimumBatteryPercent, { "$it %" }) { level ->
            viewModel.changeSettings { it.copy(converterMinimumBatteryPercent = level) }
        }
    }
}

@Composable
private fun RootChoice(viewModel: ConverterViewModel, scope: ConversionScope?) {
    val roots by viewModel.roots.collectAsState()
    if (roots.size <= 1) return
    Section(stringResource(R.string.converter_folder)) {
        Column(Modifier.selectableGroup()) {
            roots.forEach { root ->
                RadioRow(root.displayName, (scope as? ConversionScope.Root)?.rootId == root.id) { viewModel.chooseRoot(root) }
            }
        }
    }
}

@Composable
private fun Running(progress: ConversionProgress, onCancel: () -> Unit) {
    val context = LocalContext.current
    val fraction = (progress.filesDone + progress.fileFraction) / progress.filesTotal.coerceAtLeast(1)
    Text(stringResource(R.string.converter_running, progress.filesDone + 1, progress.filesTotal), color = AbbayColors.content)
    progress.fileName?.let {
        Text(it, color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA), style = MaterialTheme.typography.bodySmall)
    }
    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
    Text(
        stringResource(
            R.string.converter_saved_so_far,
            Formatter.formatShortFileSize(context, progress.savedBytes),
            Formatter.formatShortFileSize(context, progress.estimatedSavingBytes),
        ),
        color = AbbayColors.content,
    )
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.cancel), color = AbbayColors.content)
    }
}

@Composable
private fun endMessage(end: ConversionEnd, run: ConversionProgress): String {
    val saved = Formatter.formatShortFileSize(LocalContext.current, run.savedBytes)
    return when (end) {
        ConversionEnd.FINISHED -> pluralStringResource(
            R.plurals.converter_finished,
            run.filesDone - run.failedFiles,
            run.filesDone - run.failedFiles,
            saved
        ) +
            if (run.failedFiles > 0) " " + pluralStringResource(R.plurals.converter_failed_files, run.failedFiles, run.failedFiles) else ""
        ConversionEnd.CANCELLED -> stringResource(R.string.converter_cancelled)
        ConversionEnd.BATTERY_LOW -> stringResource(R.string.converter_battery_low)
        ConversionEnd.FAILED -> stringResource(R.string.converter_failed)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = AbbayColors.content, modifier = Modifier.semantics { heading() })
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(values: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label(value)) },
                colors = FilterChipDefaults.filterChipColors(labelColor = AbbayColors.content),
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(checked, role = Role.Switch) { onChange(!checked) },
    ) {
        Text(label, color = AbbayColors.content, modifier = Modifier.weight(1f))
        SettingSwitch(checked, onChange)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onSelect),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.tertiary, unselectedColor = AbbayColors.content),
        )
        Text(label, color = AbbayColors.content, modifier = Modifier.padding(start = 12.dp))
    }
}

private const val SECONDARY_ALPHA = 0.7f
