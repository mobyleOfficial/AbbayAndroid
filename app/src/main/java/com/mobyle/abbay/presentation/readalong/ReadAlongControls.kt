package com.mobyle.abbay.presentation.readalong

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.readalong.ReadAlongLanguage
import java.util.Locale

/** *English ▾* in the corner: what the text is read in, and the way to change it. */
@Composable
internal fun ReadAlongLanguageChip(
    language: ReadAlongLanguage?,
    choices: List<ReadAlongLanguage>,
    onSelect: (ReadAlongLanguage) -> Unit,
    onOpenChange: (Boolean) -> Unit,
) {
    if (language == null || choices.isEmpty()) return
    var isOpen by remember { mutableStateOf(false) }
    val description = stringResource(R.string.read_along_language)
    Box {
        Text(
            text = ReadAlongLanguageNames.of(language),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier
                .heightIn(min = TOUCH_TARGET.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = CHIP_FILL_ALPHA))
                .clickable(role = Role.Button) {
                    isOpen = true
                    onOpenChange(true)
                }
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics { contentDescription = description },
        )
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = {
                isOpen = false
                onOpenChange(false)
            },
        ) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(ReadAlongLanguageNames.of(choice)) },
                    onClick = {
                        isOpen = false
                        onOpenChange(false)
                        onSelect(choice)
                    },
                )
            }
        }
    }
}

/** A round control in a corner of the text, the language chip's look: full screen, and back out of it. */
@Composable
internal fun ReadAlongCornerButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TOUCH_TARGET.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label) { onClick() }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** The languages' names in the app's own language. */
internal object ReadAlongLanguageNames {
    @Composable
    fun of(language: ReadAlongLanguage): String {
        val locale = Locale.getDefault()
        return remember(language, locale) {
            Locale.forLanguageTag(language.code).getDisplayLanguage(locale)
                .replaceFirstChar { it.titlecase(locale) }
                .ifEmpty { language.code }
        }
    }
}
