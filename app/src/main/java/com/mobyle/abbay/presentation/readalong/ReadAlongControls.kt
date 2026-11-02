package com.mobyle.abbay.presentation.readalong

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.readalong.ReadAlongLanguage
import com.readalong.ReadAlongSnapshot

/** *English ▾* in the bottom-leading corner once the language is known; it stays when it can't be read, so another can be picked. */
@Composable
internal fun LanguageMenu(
    snapshot: ReadAlongSnapshot,
    isHidden: Boolean,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onPick: (String) -> Unit,
) {
    val language = snapshot.language ?: return
    if (snapshot.choices.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    val choices = remember(snapshot.choices, locale) { languageChoices(snapshot.choices, locale) }
    val name = ReadAlongLanguage.displayName(language, locale)
    val label = stringResource(R.string.read_along_language)
    Box(modifier = Modifier.padding(start = 4.dp)) {
        AnimatedVisibility(visible = !isHidden || isOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .sizeIn(minWidth = MIN_TOUCH, minHeight = MIN_TOUCH)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = label) { onOpenChange(true) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = label
                        stateDescription = name
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = CONTROL_FILL_ALPHA))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
        DropdownMenu(expanded = isOpen, onDismissRequest = { onOpenChange(false) }) {
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(choice.name, fontWeight = if (choice.id == language) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.semantics { selected = choice.id == language },
                    onClick = {
                        onOpenChange(false)
                        onPick(choice.id)
                    },
                )
            }
        }
    }
}

/** A round button in a corner of the text: the square's expand button, the full screen's collapse button. */
@Composable
fun ReadAlongCornerButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(MIN_TOUCH)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = CONTROL_FILL_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** The system's animations are off (Settings › Accessibility › Remove animations): the text doesn't move by itself. */
@Composable
internal fun rememberReducesMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) ==
            0f
    }
}

/** TalkBack (or any touch-exploring service) is on: the controls must not hide from it. */
@Composable
fun rememberIsTouchExplorationOn(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager }
    var isOn by remember { mutableStateOf(manager.isTouchExplorationEnabled) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { isOn = it }
        manager.addTouchExplorationStateChangeListener(listener)
        onDispose { manager.removeTouchExplorationStateChangeListener(listener) }
    }
    return isOn
}

private val MIN_TOUCH = 48.dp
private const val CONTROL_FILL_ALPHA = 0.4f
