package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A screen of rows (settings, details, lists) at a readable width on a tablet, an unfolded foldable or a phone in
 * landscape, centred on the app's background, so a label and its switch or value stay close together.
 */
@Composable
fun ReadableWidth(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.widthIn(max = READABLE_MAX_WIDTH).fillMaxHeight()) { content() }
    }
}

private val READABLE_MAX_WIDTH = 680.dp
