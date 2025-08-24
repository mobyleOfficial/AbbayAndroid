package com.mobyle.abbay.presentation

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mobyle.abbay.infra.navigation.AbbayNavHost
import com.mobyle.abbay.presentation.common.service.PlayerService
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import com.mobyle.abbay.presentation.utils.prepareBook
import com.mobyle.abbay.presentation.utils.prepareMultipleBooks
import com.model.MultipleBooks
import com.usecase.IsPlayWhenAppIsClosedEnabled
import com.usecase.UpdateAppLifeStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ExperimentalMaterialApi
@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var isPlayWhenAppIsClosedEnabled: IsPlayWhenAppIsClosedEnabled

    @Inject
    lateinit var updateAppLifeStatus: UpdateAppLifeStatus

    private val viewModel: MainViewModel by viewModels()

    private lateinit var controller: ListenableFuture<MediaController>
    private var isPlayerReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        // Keep splash screen visible until player is ready
        splashScreen.setKeepOnScreenCondition {
            !isPlayerReady
        }

        super.onCreate(savedInstanceState)

        updateAppLifeStatus(true)

        controller = MediaController.Builder(
            this,
            SessionToken(this, ComponentName(this, PlayerService::class.java))
        ).buildAsync()

        setContent {
            val navController = rememberNavController()
            val currentPlayer = remember {
                mutableStateOf<MediaController?>(null)
            }
            val selectedBook by viewModel.selectedBook.collectAsState()

            LaunchedEffect(Unit) {
                controller.addListener({
                    try {
                        currentPlayer.value = controller.get()
                    } catch (e: Exception) {
                        // Handle error but still dismiss splash screen
                        isPlayerReady = true
                    }
                }, MoreExecutors.directExecutor())
            }

            LaunchedEffect(selectedBook, currentPlayer.value) {
                (selectedBook as? BookSelectionState.Selected)?.let {
                    currentPlayer.value?.let { player ->
                        val book = it.book
                        if (!player.isPlaying) {
                            if (book is MultipleBooks) {
                                player.prepareMultipleBooks(
                                    currentPosition = book.currentBookPosition,
                                    idList = book.bookFileList.map { it.id },
                                    progress = book.progress,
                                    isPlaying = MutableStateFlow(false)
                                )
                            } else {
                                player.prepareBook(
                                    id = book.id,
                                    progress = book.progress,
                                    isPlaying = MutableStateFlow(false)
                                )
                            }
                        }
                    }
                }
            }

            LaunchedEffect(currentPlayer.value) {
                currentPlayer.value?.addListener(
                    object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_READY) {
                                isPlayerReady = true
                            }
                        }
                    }
                )
            }

            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    currentPlayer.value?.let { player ->
                        AbbayNavHost(
                            player = player,
                            navController = navController
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        updateAppLifeStatus(false)

        if (!isPlayWhenAppIsClosedEnabled()) {
            if (::controller.isInitialized) {
                controller.get().stop()
                controller.get().release()
            }
        }
    }
}
