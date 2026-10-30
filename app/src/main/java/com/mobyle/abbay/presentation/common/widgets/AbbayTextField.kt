package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.mobyle.abbay.presentation.common.theme.AbbayColors

@Composable
fun abbayTextFieldColors(): TextFieldColors {
    val accent = MaterialTheme.colorScheme.tertiary
    val faded = AbbayColors.content.copy(alpha = UNFOCUSED_LABEL_ALPHA)
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = AbbayColors.content,
        unfocusedTextColor = AbbayColors.content,
        focusedContainerColor = AbbayColors.content.copy(alpha = FIELD_FILL_ALPHA),
        unfocusedContainerColor = AbbayColors.content.copy(alpha = FIELD_FILL_ALPHA),
        cursorColor = accent,
        selectionColors = TextSelectionColors(handleColor = accent, backgroundColor = accent.copy(alpha = SELECTION_ALPHA)),
        focusedBorderColor = accent,
        unfocusedBorderColor = AbbayColors.content.copy(alpha = UNFOCUSED_BORDER_ALPHA),
        focusedLabelColor = accent,
        unfocusedLabelColor = faded,
        focusedPlaceholderColor = faded,
        unfocusedPlaceholderColor = faded,
        focusedLeadingIconColor = AbbayColors.content,
        unfocusedLeadingIconColor = AbbayColors.content,
        focusedTrailingIconColor = AbbayColors.content,
        unfocusedTrailingIconColor = AbbayColors.content,
        focusedSupportingTextColor = faded,
        unfocusedSupportingTextColor = faded,
    )
}

private const val FIELD_FILL_ALPHA = 0.05f
private const val SELECTION_ALPHA = 0.4f
private const val UNFOCUSED_BORDER_ALPHA = 0.5f
private const val UNFOCUSED_LABEL_ALPHA = 0.75f
