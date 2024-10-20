package com.mobyle.abbay.infra.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
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
        composable(
            route = NavigationItem.BookList.route,
        ) {
            BooksListScreen(
                player = player,
            ) {
                navController.navigate(NavigationItem.Settings.route)
            }
        }

        modal(NavigationItem.Settings.route) {
            SettingsScreen()
        }
    }
}

private fun NavGraphBuilder.modal(route: String, screen: @Composable () -> Unit) {
    composable(
        route = route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Up,
                animationSpec = tween(200)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Down,
                animationSpec = tween(200)
            )
        }
    ) {
        screen()
    }
}
