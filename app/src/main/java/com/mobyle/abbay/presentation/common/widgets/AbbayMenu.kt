package com.mobyle.abbay.presentation.common.widgets

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import com.mobyle.abbay.presentation.common.theme.AbbayColors

@Composable
fun AbbayDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.surface,
    content = content,
)

@Composable
fun AbbayMenuItem(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    @DrawableRes leadingIconResource: Int? = null,
    trailingIcon: (@Composable (tint: Color) -> Unit)? = null,
) {
    val color = when {
        !enabled -> AbbayColors.content.copy(alpha = DISABLED_ALPHA)
        selected -> MaterialTheme.colorScheme.tertiary
        else -> AbbayColors.content
    }
    val leadingVector = leadingIcon ?: leadingIconResource?.let { ImageVector.vectorResource(it) }
    DropdownMenuItem(
        text = { Text(text, color = color, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        onClick = onClick,
        enabled = enabled,
        leadingIcon = leadingVector?.let { vector -> { Icon(vector, contentDescription = null, tint = color) } },
        trailingIcon = when {
            trailingIcon != null -> {
                { trailingIcon(color) }
            }
            selected -> {
                { Icon(Icons.Default.Check, contentDescription = null, tint = color) }
            }
            else -> null
        },
    )
}

private const val DISABLED_ALPHA = 0.5f
