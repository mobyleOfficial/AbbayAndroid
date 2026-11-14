package com.mobyle.abbay.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.utils.debounceClick

/**
 * One row of Settings. [info] is a plain explanation of what the setting does, for anyone the label alone leaves
 * guessing; it puts an (i) next to the label that opens it.
 */
@Composable
fun SettingItem(
    text: String,
    onClick: (() -> Unit)? = null,
    info: String? = null,
    content: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val clickModifier = if (onClick == null) Modifier else Modifier.debounceClick { onClick() }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickModifier)
                .heightIn(min = 52.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AbbayColors.content,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(start = 16.dp, end = if (info == null) 16.dp else 0.dp, top = 8.dp, bottom = 8.dp)
                )
                info?.let { SettingInfoButton(title = text, info = it) }
            }
            content?.invoke()
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = AbbayColors.content.copy(alpha = DIVIDER_ALPHA),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun SettingInfoButton(title: String, info: String) {
    var isOpen by remember { mutableStateOf(false) }
    IconButton(onClick = { isOpen = true }) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = stringResource(R.string.settings_info_description, title),
            tint = AbbayColors.content.copy(alpha = INFO_ICON_ALPHA),
            modifier = Modifier.size(20.dp),
        )
    }
    if (isOpen) {
        AbbayAlertDialog(
            onDismissRequest = { isOpen = false },
            title = { Text(title) },
            text = { Text(info) },
            confirmButton = { AbbayTextButton(onClick = { isOpen = false }) { Text(stringResource(R.string.got_it)) } },
        )
    }
}

private const val DIVIDER_ALPHA = 0.12f
private const val INFO_ICON_ALPHA = 0.6f
