package com.mobyle.abbay.presentation

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.common.util.concurrent.MoreExecutors
import com.mobyle.abbay.presentation.booklist.BooksListScreen
import com.mobyle.abbay.presentation.common.service.PlayerService
import com.mobyle.abbay.presentation.common.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

@ExperimentalMaterialApi
@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller =
            MediaController.Builder(
                this,
                SessionToken(this, ComponentName(this, PlayerService::class.java))
            ).buildAsync()

        controller.addListener({
            setContent {
                MyApplicationTheme {
                    // A surface container using the 'background' color from the theme
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        BooksListScreen(controller.get())
                    }
                }
            }
        }, MoreExecutors.directExecutor())
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {

    }
}