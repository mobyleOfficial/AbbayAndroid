package com.mobyle.abbay.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.session.MediaController
import androidx.navigation.compose.rememberNavController
import androidx.startup.AppInitializer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.common.util.concurrent.ListenableFuture
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

        // Keep splash screen visible until player is ready
        splashScreen.setKeepOnScreenCondition {
            !(isPlayerReady)
        }

        super.onCreate(savedInstanceState)

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
                    modifier = Modifier.fillMaxSize(),
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
