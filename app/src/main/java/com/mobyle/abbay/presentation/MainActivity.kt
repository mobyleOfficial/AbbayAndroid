package com.mobyle.abbay.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mobyle.abbay.presentation.booklist.PermissionsHandlerScreen
import com.mobyle.abbay.presentation.common.theme.AbbayTheme

@ExperimentalPermissionsApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbbayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionsHandlerScreen()
                }
            }
        }
    }
}

@ExperimentalPermissionsApi
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AbbayTheme {
        PermissionsHandlerScreen()
    }
}