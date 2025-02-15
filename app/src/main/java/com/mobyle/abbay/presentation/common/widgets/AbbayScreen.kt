package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


@Composable
fun AbbayScreen(
    title: String,
    onClose: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            backgroundColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    title,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            actions = {
                if (onClose != null) {
                    IconButton(onClick = { onClose() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "",
                            tint = Color.White
                        )
                    }
                }
            },
        )
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(innerPadding)
        ) {
            content()
        }
    }
}