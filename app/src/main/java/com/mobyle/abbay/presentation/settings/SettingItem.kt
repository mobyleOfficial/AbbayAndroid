package com.mobyle.abbay.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.utils.debounceClick

@Composable
fun SettingItem(
    text: String,
    onClick: (() -> Unit)? = null,
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
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = AbbayColors.content,
                // The label takes the room left by the control, so a long label wraps instead of pushing it off screen.
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp)
            )
            content?.invoke()
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = AbbayColors.content.copy(alpha = DIVIDER_ALPHA),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

private const val DIVIDER_ALPHA = 0.12f
