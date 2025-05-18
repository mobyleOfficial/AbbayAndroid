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
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = Modifier.background(MaterialTheme.colorScheme.primary),
        onClick = onClick
    ) {
        SVGIcon(
            path = imageUrl,
            description = ""
        )
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}