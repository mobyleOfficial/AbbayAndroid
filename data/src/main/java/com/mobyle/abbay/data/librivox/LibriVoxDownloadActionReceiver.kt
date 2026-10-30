package com.mobyle.abbay.data.librivox

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LibriVoxDownloadActionReceiver : BroadcastReceiver() {
    @Inject lateinit var control: LibriVoxDownloadControl

    override fun onReceive(context: Context, intent: Intent) {
        val bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L).takeIf { it >= 0 } ?: return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_PAUSE -> control.pause(bookId)
                    ACTION_CANCEL -> control.remove(bookId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_PAUSE = "com.mobyle.abbay.librivox.PAUSE"
        private const val ACTION_CANCEL = "com.mobyle.abbay.librivox.CANCEL"
        private const val EXTRA_BOOK_ID = "bookId"
        private const val CANCEL_REQUEST_OFFSET = 1_000_000
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun pauseIntent(context: Context, bookId: Long) = pendingIntent(context, bookId, ACTION_PAUSE, bookId.toInt())

        fun cancelIntent(context: Context, bookId: Long) =
            pendingIntent(context, bookId, ACTION_CANCEL, bookId.toInt() + CANCEL_REQUEST_OFFSET)

        private fun pendingIntent(context: Context, bookId: Long, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, LibriVoxDownloadActionReceiver::class.java).setAction(action).putExtra(EXTRA_BOOK_ID, bookId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
