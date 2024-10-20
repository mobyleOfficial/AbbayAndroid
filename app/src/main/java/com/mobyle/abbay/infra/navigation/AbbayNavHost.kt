package com.mobyle.abbay.infra.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobyle.abbay.presentation.booklist.BooksListScreen
import com.mobyle.abbay.presentation.folder.FolderScreen
import com.mobyle.abbay.presentation.settings.SettingsScreen

@Composable
fun AbbayNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    player: MediaController,
    startDestination: String = NavigationItem.BookList.route,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavigationItem.BookList.route) {
            BooksListScreen(player = player)
        }
        composable(NavigationItem.Settings.route) {
            SettingsScreen()
        }
        composable(NavigationItem.Folder.route) {
            FolderScreen()
        }
    }
}