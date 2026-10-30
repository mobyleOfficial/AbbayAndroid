package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors

@Composable
fun AbbayScreen(
    title: String,
    onClose: (() -> Unit)? = null,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(scaffoldState = scaffoldState, topBar = {
        TopAppBar(
            backgroundColor = MaterialTheme.colorScheme.primary,
            elevation = 0.dp,
            title = {
                Text(
                    title,
                    style = TextStyle(
                        color = AbbayColors.content,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            actions = {
                actions()
                if (onClose != null) {
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                        onClick = onClose,
                    )
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
