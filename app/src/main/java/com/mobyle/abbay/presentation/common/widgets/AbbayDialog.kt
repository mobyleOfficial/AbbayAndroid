package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties
import com.mobyle.abbay.presentation.common.theme.AbbayColors

@Composable
fun AbbayAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) = AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = confirmButton,
    modifier = modifier,
    dismissButton = dismissButton,
    icon = icon,
    title = title,
    text = text,
    containerColor = MaterialTheme.colorScheme.surface,
    iconContentColor = AbbayColors.content,
    titleContentColor = AbbayColors.content,
    textContentColor = AbbayColors.content,
    properties = properties,
)

@Composable
fun AbbayTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = abbayTextButtonColors(),
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit,
) = TextButton(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    colors = colors,
    contentPadding = contentPadding,
) {
    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(color = Color.Unspecified)) { content() }
}

@Composable
fun abbayTextButtonColors(isDestructive: Boolean = false): ButtonColors = ButtonDefaults.textButtonColors(
    contentColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
    disabledContentColor = AbbayColors.content.copy(alpha = DISABLED_ALPHA),
)

private const val DISABLED_ALPHA = 0.38f
