package com.mobyle.abbay.presentation.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import com.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

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
): Flow<T> {
    return remember(flow, lifecycleOwner) {
        flow.flowWithLifecycle(
            lifecycleOwner.lifecycle,
            Lifecycle.State.STARTED
        )
    }
}

// To understand its use read the following article:
// https://euryperez.dev/consuming-flows-safely-in-non-composable-scopes-in-jetpack-compose-d0154565bd68
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

fun Cursor.getTitle(): String? {
    return getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.TITLE))
}

fun Cursor.getAlbumTitle(): String? {
    return getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM))
}

fun Cursor.getFileName(): String? {
    return getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns.DISPLAY_NAME))
}

fun Cursor.getId(): String? {
    return getStringOrNull(getColumnIndex(MediaStore.Audio.AudioColumns._ID))
}

fun Cursor.getDuration(): Long? {
    return getLongOrNull(getColumnIndex(MediaStore.Audio.Media.DURATION))
}


@androidx.annotation.OptIn(UnstableApi::class)
fun MediaController.playBook(
    id: String,
    progress: Long,
    isPlaying: MutableStateFlow<Boolean>
) {
    prepareBook(id, progress, isPlaying)
    isPlaying.value = true
    playWhenReady = true
}

@androidx.annotation.OptIn(UnstableApi::class)
fun MediaController.playMultipleBooks(
    currentPosition: Int,
    idList: List<String>,
    progress: Long,
    isPlaying: MutableStateFlow<Boolean>
) {
    prepareMultipleBooks(
        currentPosition = currentPosition,
        idList = idList,
        progress = progress,
        isPlaying = isPlaying
    )
    isPlaying.value = true
    playWhenReady = true
}

@androidx.annotation.OptIn(UnstableApi::class)
fun MediaController.prepareBook(
    id: String,
    progress: Long,
    isPlaying: MutableStateFlow<Boolean>,
) {
    isPlaying.value = false
    pause()
    clearMediaItems()
    val uri =
        Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + File.separatorChar + id)
    val mediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .build()
    addMediaItem(mediaItem)
    seekTo(progress)
    prepare()
}

fun MediaController.prepareMultipleBooks(
    currentPosition: Int,
    idList: List<String>,
    progress: Long,
    isPlaying: MutableStateFlow<Boolean>,
) {
    isPlaying.value = false
    pause()
    clearMediaItems()

    val items = idList.map { id ->
        val uri =
            Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + File.separatorChar + id)
        val mediaItem = MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .build()
        mediaItem
    }
    addMediaItems(items)
    seekTo(currentPosition, progress)
    prepare()
}

fun Context.musicCursor(block: (Cursor) -> Unit) {
    contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
        MediaStore.Audio.Media.DEFAULT_SORT_ORDER
    )
        ?.use { cursor ->
            while (cursor.moveToNext()) {
                block.invoke(cursor)
            }
        }
}

fun List<Book>.intermediateProgress(index: Int) = this.map { it.duration }.subList(0,index).sum()
