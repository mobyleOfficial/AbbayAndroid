package com.mobyle.abbay.presentation.diagnostics

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.diagnostics.DiagnosticsReport
import com.diagnostics.DiagnosticsText
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Device, decoders, audio output and the playback log, shareable as text. */
@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel = hiltViewModel(), close: () -> Unit) {
    val report by viewModel.report.collectAsState()
    val includeFileNames by viewModel.includeFileNames.collectAsState()
    val context = LocalContext.current

    AbbayScreen(
        title = stringResource(R.string.diagnostics_title),
        onClose = close,
        actions = {
            report?.let { current ->
                IconButton(onClick = { share(context, current, includeFileNames) }) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.diagnostics_share), tint = AbbayColors.content)
                }
            }
        },
    ) {
        val current = report ?: return@AbbayScreen
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { Facts(current) }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(includeFileNames, role = Role.Checkbox, onValueChange = viewModel::setIncludeFileNames)
                        .padding(horizontal = 16.dp),
                ) {
                    Checkbox(
                        checked = includeFileNames,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.tertiary,
                            uncheckedColor = AbbayColors.content
                        ),
                    )
                    Text(stringResource(R.string.diagnostics_include_file_names), color = AbbayColors.content)
                }
            }
            item { Heading(pluralStringResource(R.plurals.diagnostics_log, current.log.size, current.log.size)) }
            items(current.log.asReversed()) { line ->
                Text(
                    "${time(line.atMs)}  ${DiagnosticsText.lineText(line, includeFileNames)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = AbbayColors.content,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun Facts(report: DiagnosticsReport) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Fact(stringResource(R.string.diagnostics_android), report.androidVersion)
        Fact(stringResource(R.string.diagnostics_device), report.device)
        Fact(stringResource(R.string.diagnostics_app), report.appVersion)
        Fact(
            stringResource(R.string.diagnostics_audio_output),
            stringResource(R.string.diagnostics_audio_output_value, report.outputSampleRate ?: 0, report.outputFramesPerBuffer ?: 0),
        )
        Fact(stringResource(R.string.diagnostics_opus_encoder), report.opusEncoder ?: stringResource(R.string.diagnostics_opus_missing))
        Heading(stringResource(R.string.diagnostics_decoders), horizontalPadding = 0.dp)
        report.decoders.forEach { (mimeType, names) ->
            Fact(mimeType, names.joinToString().ifEmpty { stringResource(R.string.diagnostics_none) })
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AbbayColors.content.copy(alpha = LABEL_ALPHA))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AbbayColors.content)
    }
}

@Composable
private fun Heading(text: String, horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = AbbayColors.content,
        modifier = Modifier.padding(top = 8.dp, start = horizontalPadding, end = horizontalPadding).semantics { heading() },
    )
}

/** 24-hour time with seconds: log lines are seconds apart. */
private fun time(atMs: Long): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(atMs))

private fun share(context: Context, report: DiagnosticsReport, includeFileNames: Boolean) {
    val text = DiagnosticsText.of(report, includeFileNames) { time(it) }
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.diagnostics_title))
    context.startActivity(Intent.createChooser(send, context.getString(R.string.diagnostics_share)))
}

private const val LABEL_ALPHA = 0.7f
