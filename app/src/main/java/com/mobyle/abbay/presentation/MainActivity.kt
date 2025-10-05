package com.mobyle.abbay.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.session.MediaController
import androidx.navigation.compose.rememberNavController
import androidx.startup.AppInitializer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mobyle.abbay.infra.navigation.AbbayNavHost
import com.mobyle.abbay.infra.startup.PlayerInitializer
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import com.usecase.IsPlayWhenAppIsClosedEnabled
import com.usecase.UpdateAppLifeStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@ExperimentalMaterialApi
@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var isPlayWhenAppIsClosedEnabled: IsPlayWhenAppIsClosedEnabled

    @Inject
    lateinit var updateAppLifeStatus: UpdateAppLifeStatus

    private var player: MediaController? = null
    private var isPlayerReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        // Keep splash screen visible until player is ready
        splashScreen.setKeepOnScreenCondition {
            !isPlayerReady
        }

        super.onCreate(savedInstanceState)

//        // Enable edge-to-edge mode
//        // This disables the default system behavior where the window content is adjusted to fit within the system bars (status and navigation bars).
//        // By setting this to false, your app's content is allowed to draw under the system bars, creating an immersive, edge-to-edge experience.
//        // You can then manage how your content interacts with these bars using insets and modifiers.
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//
//        // Adjust status and navigation bar appearance
//        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
//        insetsController.isAppearanceLightStatusBars = true
//        insetsController.isAppearanceLightNavigationBars = true
//
//        // Optional: Set specific colors for status and navigation bars
//        window.statusBarColor = android.graphics.Color.TRANSPARENT
//        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        updateAppLifeStatus(true)

        setContent {
            val navController = rememberNavController()
            val playerState = AppInitializer.getInstance(this@MainActivity)
                .initializeComponent(PlayerInitializer::class.java).collectAsState(null)

            LaunchedEffect(playerState.value) {
                player = playerState.value
                isPlayerReady = playerState.value != null
            }

            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    playerState.value?.let { player ->
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
            if (player != null) {
                player?.stop()
                player?.release()
            }
        }
    }


}
