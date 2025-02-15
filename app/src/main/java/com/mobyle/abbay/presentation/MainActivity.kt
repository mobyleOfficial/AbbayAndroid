package com.mobyle.abbay.presentation

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mobyle.abbay.infra.navigation.AbbayNavHost
import com.mobyle.abbay.presentation.common.service.PlayerService
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import com.usecase.IsPlayWhenAppIsClosedEnabled
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@ExperimentalMaterialApi
@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var isPlayWhenAppIsClosedEnabled: IsPlayWhenAppIsClosedEnabled
    private lateinit var controller: ListenableFuture<MediaController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller  =
            MediaController.Builder(
                this,
                SessionToken(this, ComponentName(this, PlayerService::class.java))
            ).buildAsync()

        controller.addListener({
            setContent {
                val navController = rememberNavController()

                MyApplicationTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AbbayNavHost(
                            player = controller.get(),
                            navController = navController
                        )
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!isPlayWhenAppIsClosedEnabled()) {
            if(::controller.isInitialized) {
                controller.get().stop()
                controller.get().release()
            }
        }
    }
}
