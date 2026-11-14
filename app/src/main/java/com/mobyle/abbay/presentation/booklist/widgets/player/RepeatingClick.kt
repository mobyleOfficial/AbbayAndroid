package com.mobyle.abbay.presentation.booklist.widgets.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Modifier.repeatingClick(label: String, onClick: () -> Unit): Modifier = composed {
    val currentOnClick = rememberUpdatedState(onClick)
    val scope = rememberCoroutineScope()
    semantics {
        role = Role.Button
        contentDescription = label
        onClick(label) {
            currentOnClick.value()
            true
        }
    }.pointerInput(Unit) {
        val longPressTimeoutMs = viewConfiguration.longPressTimeoutMillis
        detectTapGestures(
            onPress = {
                val repeater = scope.launch {
                    delay(longPressTimeoutMs)
                    while (true) {
                        currentOnClick.value()
                        delay(REPEAT_INTERVAL_MS)
                    }
                }
                tryAwaitRelease()
                repeater.cancel()
            },
            onLongPress = {},
            onTap = { currentOnClick.value() },
        )
    }
}

private const val REPEAT_INTERVAL_MS = 500L
