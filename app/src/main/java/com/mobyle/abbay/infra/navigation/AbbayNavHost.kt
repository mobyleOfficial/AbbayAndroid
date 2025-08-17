package com.mobyle.abbay.infra.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.session.MediaController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mobyle.abbay.presentation.booklist.BooksListScreen
import com.mobyle.abbay.presentation.booklist.BooksListViewModel
import com.mobyle.abbay.presentation.settings.SettingsScreen

@Composable
fun AbbayNavHost(
    viewModel: BooksListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    navController: NavHostController,
    player: MediaController,
    startDestination: String = NavigationItem.BookList.route,
) {
    val context = LocalContext.current

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = startDestination
    ) {
        composable(
            route = NavigationItem.BookList.route,
        ) {
            BooksListScreen(
                viewModel = viewModel,
                player = player,
                openAppSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                },
                navigateToSettings = { navController.navigate(NavigationItem.Settings.route) }
            )
        }

        modal(NavigationItem.Settings.route) {
            SettingsScreen(
                booksViewModel = viewModel
            ) {
                navController.popBackStack()
            }
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
