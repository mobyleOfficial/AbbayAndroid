package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AbbaysDropdownItem(
    text: String,
    imageUrl: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val color = Color.White.copy(alpha = if (enabled) 1f else 0.5f)

    DropdownMenuItem(
        modifier = Modifier.background(MaterialTheme.colorScheme.primary),
        onClick = onClick,
        enabled = enabled
    ) {
        SVGIcon(
            path = imageUrl,
            description = "",
            color = color,
        )
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}