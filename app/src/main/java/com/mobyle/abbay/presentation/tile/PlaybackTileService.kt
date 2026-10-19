package com.mobyle.abbay.presentation.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.mobyle.abbay.R
import com.player.PlaybackStatus
import com.player.PlayerController
import com.usecase.GetLastBook
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Quick Settings tile: play/pause of the last book; long press opens the player
 * (`QS_TILE_PREFERENCES` on MainActivity). The tile shows "active" while a book plays.
 */
@AndroidEntryPoint
class PlaybackTileService : TileService() {
    @Inject
    lateinit var playerController: PlayerController

    @Inject
    lateinit var getLastBook: GetLastBook

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var listening: Job? = null

    override fun onStartListening() {
        playerController.connect()
        listening = playerController.state.onEach { state ->
            qsTile?.apply {
                this.state = if (state.isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = getString(R.string.tile_label)
                updateTile()
            }
        }.launchIn(scope)
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
    }

    /** The system unbinds the tile right after a click, so the work runs in [clickScope], which outlives this service. */
    override fun onClick() {
        clickScope.launch {
            playerController.connect()
            val state = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { playerController.state.first { it.isConnected } } ?: return@launch
            when {
                state.isPlaying -> playerController.pause()
                state.status == PlaybackStatus.READY && state.trackCount > 0 -> playerController.play()
                else -> getLastBook()?.let { book -> playerController.openBook(book, playWhenReady = true) }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 5_000L
        val clickScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}
