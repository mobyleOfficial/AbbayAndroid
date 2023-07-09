package com.mobyle.abbay.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mobyle.abbay.presentation.booklist.BooksListScreen
import com.mobyle.abbay.presentation.common.theme.AbbayTheme
import dagger.hilt.android.AndroidEntryPoint

@ExperimentalMaterial3Api
@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbbayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    BooksListScreen()
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@ExperimentalPermissionsApi
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AbbayTheme {
        BooksListScreen()
    }
}