package com.mobyle.abbay.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.mobyle.abbay.R
import com.mobyle.abbay.infra.common.ChargingScreenKeeper
import com.mobyle.abbay.infra.language.AppLanguageStore
import com.mobyle.abbay.infra.navigation.AbbayNavHost
import com.mobyle.abbay.presentation.booklist.BooksListViewModel
import com.mobyle.abbay.presentation.common.theme.LocalStatusBarColor
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import com.mobyle.abbay.presentation.common.theme.StatusBarColor
import com.mobyle.abbay.presentation.common.theme.statusBarBackground
import com.model.DataResult
import com.player.PlayerController
import com.settings.AppSettings
import com.usecase.ApplySharedCover
import com.usecase.ObserveAppSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterialApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var observeAppSettings: ObserveAppSettings

    @Inject
    lateinit var playerController: PlayerController

    @Inject
    lateinit var applySharedCover: ApplySharedCover

    private val booksViewModel: BooksListViewModel by viewModels()

    private var isPlayerReady = false
    private var settings = AppSettings()
    private val chargingScreenKeeper = ChargingScreenKeeper(this)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        lockPhonesToPortrait()
        splashScreen.setKeepOnScreenCondition {
            !isPlayerReady
        }

        super.onCreate(savedInstanceState)

        lifecycle.addObserver(chargingScreenKeeper)
        observeAppSettings().onEach {
            settings = it
            chargingScreenKeeper.setEnabled(it.keepScreenOnWhileCharging)
        }.launchIn(lifecycleScope)
        playerController.connect()
        handleSharedImage(intent)
        if (savedInstanceState == null) handleLaunchAction(intent)

        setContent {
            val navController = rememberNavController()
            val playerState = playerController.state.collectAsState()
            val appSettings = remember { observeAppSettings() }.collectAsState(initial = settings)

            LaunchedEffect(playerState.value.isConnected) {
                isPlayerReady = playerState.value.isConnected
            }

            MyApplicationTheme(themeMode = appSettings.value.themeMode, dynamicColor = appSettings.value.dynamicColor) {
                val statusBarColor = remember { StatusBarColor() }
                CompositionLocalProvider(LocalStatusBarColor provides statusBarColor) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsTopHeight(WindowInsets.statusBars)
                                    .statusBarBackground(statusBarColor)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .consumeWindowInsets(WindowInsets.statusBars)
                                    .windowInsetsPadding(
                                        WindowInsets.systemBars.only(
                                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                                        )
                                    )
                            ) {
                                if (playerState.value.isConnected) {
                                    AbbayNavHost(
                                        viewModel = booksViewModel,
                                        navController = navController
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedImage(intent)
        handleLaunchAction(intent)
    }

    private fun handleLaunchAction(intent: Intent?) {
        when (intent?.action) {
            ACTION_RESUME_LAST_BOOK -> booksViewModel.resumeLastBook()
            ACTION_START_SLEEP_TIMER -> booksViewModel.resumeLastBook(startSleepTimer = true)
            TileService.ACTION_QS_TILE_PREFERENCES -> booksViewModel.openPlayer()
            Intent.ACTION_VIEW -> intent.data?.takeIf { intent.type?.startsWith("audio/") != false }?.let { uri ->
                booksViewModel.playExternalFile(uri.toString())
            }
        }
    }

    private fun handleSharedImage(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type?.startsWith("image/") != true) return
        val imageUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: return
        lifecycleScope.launch {
            val messageResource = when (applySharedCover(imageUri.toString())) {
                is DataResult.Success -> R.string.details_cover_updated
                is DataResult.Failure -> R.string.details_cover_not_updated
            }
            Toast.makeText(this@MainActivity, messageResource, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Phones (and a folded foldable) stay in portrait; a tablet or an unfolded foldable follows how it is held, and
     * Android 16 ignores a lock on large screens anyway. Decided from the display, not the window, so a small window
     * on a tablet is not reshaped; a fold recreates the activity and decides again.
     */
    private fun lockPhonesToPortrait() {
        requestedOrientation = if (displaySmallestWidthDp() < LARGE_SCREEN_MIN_WIDTH_DP) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun displaySmallestWidthDp(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.maximumWindowMetrics.bounds
        (minOf(bounds.width(), bounds.height()) / resources.displayMetrics.density).toInt()
    } else {
        resources.configuration.smallestScreenWidthDp
    }

    companion object {
        private const val LARGE_SCREEN_MIN_WIDTH_DP = 600
        const val ACTION_RESUME_LAST_BOOK = "com.mobyle.abbay.action.RESUME_LAST_BOOK"
        const val ACTION_START_SLEEP_TIMER = "com.mobyle.abbay.action.START_SLEEP_TIMER"
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isFinishing && !settings.playWhenAppIsClosed) {
            playerController.stop()
            playerController.release()
        }
    }
}
