package com.mobyle.abbay.presentation.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.model.Book
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterialApi::class)
val BottomSheetScaffoldState.currentFraction: Float
    get() {
        val fraction = bottomSheetState.progress
        val targetValue = bottomSheetState.targetValue
        val currentValue = bottomSheetState.currentValue

        return when {
            currentValue == BottomSheetValue.Collapsed && targetValue == BottomSheetValue.Collapsed -> 0f
            currentValue == BottomSheetValue.Expanded && targetValue == BottomSheetValue.Expanded -> 1f
            currentValue == BottomSheetValue.Collapsed && targetValue == BottomSheetValue.Expanded -> fraction
            else -> 1f - fraction
        }
    }

fun Long.toHHMMSS(): String {
    if (this == 0L) {
        return "00:00:00"
    }
    val hours = this / (1000 * 60 * 60)
    val minutes = (this % (1000 * 60 * 60)) / (1000 * 60)
    val seconds = ((this % (1000 * 60 * 60)) % (1000 * 60)) / 1000
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
private fun <T> rememberFlowWithLifecycle(
    flow: Flow<T>,
    lifecycleOwner: LifecycleOwner
): Flow<T> = remember(flow, lifecycleOwner) {
    flow.flowWithLifecycle(
        lifecycleOwner.lifecycle,
        Lifecycle.State.STARTED
    )
}

@Composable
fun <T> LaunchedEffectAndCollect(
    flow: Flow<T?>,
    function: suspend (value: T?) -> Unit
) {
    val effectFlow =
        rememberFlowWithLifecycle(flow, LocalLifecycleOwner.current)

    LaunchedEffect(effectFlow) {
        effectFlow.collect(function)
    }
}

fun List<Book>.intermediateProgress(index: Int) = this.map { it.duration }.subList(0, index).sum()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.debounceClick(
    debounceTimeMs: Long = 300L,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val debouncedClick = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= debounceTimeMs) {
            lastClickTime = currentTime
            onClick()
        }
    }

    return if (onLongClick == null) {
        this.clickable(onClick = debouncedClick)
    } else {
        this.combinedClickable(onClick = debouncedClick, onLongClick = onLongClick)
    }
}
