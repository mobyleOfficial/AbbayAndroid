package com.mobyle.abbay.infra.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.library.FolderLocation
import com.mobyle.abbay.presentation.about.LicensesScreen
import com.mobyle.abbay.presentation.bookdetails.BookDetailsScreen
import com.mobyle.abbay.presentation.booklist.AdaptiveLibraryShell
import com.mobyle.abbay.presentation.booklist.BooksListScreen
import com.mobyle.abbay.presentation.booklist.BooksListViewModel
import com.mobyle.abbay.presentation.booklist.LibraryPlayerDependencies
import com.mobyle.abbay.presentation.booklist.LibraryPlayerHost
import com.mobyle.abbay.presentation.booklist.PlayerNavigation
import com.mobyle.abbay.presentation.bookmarks.BookmarksScreen
import com.mobyle.abbay.presentation.characters.CharactersScreen
import com.mobyle.abbay.presentation.converter.ConverterScreen
import com.mobyle.abbay.presentation.diagnostics.DiagnosticsScreen
import com.mobyle.abbay.presentation.documents.CompanionDocumentsViewModel
import com.mobyle.abbay.presentation.effects.AudioEffectsViewModel
import com.mobyle.abbay.presentation.folders.FolderScreen
import com.mobyle.abbay.presentation.folders.FolderViewModel
import com.mobyle.abbay.presentation.history.HistoryScreen
import com.mobyle.abbay.presentation.librivox.LibriVoxBookScreen
import com.mobyle.abbay.presentation.librivox.LibriVoxBookViewModel
import com.mobyle.abbay.presentation.librivox.LibriVoxSearchScreen
import com.mobyle.abbay.presentation.librivox.LibriVoxSearchViewModel
import com.mobyle.abbay.presentation.options.PlaybackOptionsViewModel
import com.mobyle.abbay.presentation.queue.QueueScreen
import com.mobyle.abbay.presentation.search.SearchScreen
import com.mobyle.abbay.presentation.settings.SettingsScreen
import com.mobyle.abbay.presentation.statistics.BookStatisticsScreen
import com.mobyle.abbay.presentation.statistics.StatisticsScreen
import com.mobyle.abbay.presentation.subtitles.TextPaneScreen

@Composable
fun AbbayNavHost(
    viewModel: BooksListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = NavigationItem.BookList.route,
    effectsViewModel: AudioEffectsViewModel = hiltViewModel(),
    optionsViewModel: PlaybackOptionsViewModel = hiltViewModel(),
    documentsViewModel: CompanionDocumentsViewModel = hiltViewModel(),
) {
    val destinations = LibraryDestinations(
        queue = { navController.navigate(NavigationItem.Queue.route) },
        search = { navController.navigate(NavigationItem.Search.route) },
        librivox = { navController.navigate(NavigationItem.LibriVox.routeFor()) },
        folders = { location -> navController.navigate(NavigationItem.Folders.routeFor(location)) },
        statistics = { navController.navigate(NavigationItem.Statistics.route) },
        bookStatistics = { bookId -> navController.navigate(NavigationItem.BookStatistics.routeFor(bookId)) },
        characters = { bookId -> navController.navigate(NavigationItem.Characters.routeFor(bookId)) },
        text = { bookId -> navController.navigate(NavigationItem.Text.routeFor(bookId)) },
    )
    val navigateToSettings = { navController.navigate(NavigationItem.Settings.route) }
    val navigateToBookDetails = { bookId: String -> navController.navigate(NavigationItem.BookDetails.routeFor(bookId)) }
    // Above the screens, so the player pane (on a tablet or an unfolded foldable) stays beside any of them.
    val player = LibraryPlayerDependencies(
        viewModel = viewModel,
        effectsViewModel = effectsViewModel,
        optionsViewModel = optionsViewModel,
        documentsViewModel = documentsViewModel,
        navigation = PlayerNavigation(
            settings = navigateToSettings,
            bookDetails = navigateToBookDetails,
            bookmarks = { bookId -> navController.navigate(NavigationItem.Bookmarks.routeFor(bookId)) },
            history = { bookId -> navController.navigate(NavigationItem.History.routeFor(bookId)) },
            destinations = destinations,
        ),
    )
    AdaptiveLibraryShell(player) { host ->
        AbbayScreens(viewModel, host, player, navController, startDestination, modifier)
    }
}

@Composable
private fun AbbayScreens(
    viewModel: BooksListViewModel,
    host: LibraryPlayerHost,
    player: LibraryPlayerDependencies,
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier,
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
                viewModel = viewModel,
                host = host,
                player = player,
                navigateToSettings = player.navigation.settings,
                navigateToBookDetails = player.navigation.bookDetails,
                navigateTo = player.navigation.destinations,
            )
        }

        libraryExtras(viewModel, navController)

        libriVoxScreens(viewModel, navController)

        modal(NavigationItem.BookDetails.route) {
            BookDetailsScreen(
                onConvert = viewModel.converterEntry()?.let {
                    { bookId -> navController.navigate(NavigationItem.Converter.routeFor(bookId)) }
                },
            ) { navController.popBackStack() }
        }

        modal(NavigationItem.Bookmarks.route) {
            BookmarksScreen(
                onJump = { bookPositionMs ->
                    viewModel.seekToBookPosition(bookPositionMs)
                    navController.popBackStack()
                },
                close = { navController.popBackStack() },
            )
        }

        modal(NavigationItem.History.route) {
            HistoryScreen(
                onJump = { bookPositionMs ->
                    viewModel.seekToBookPosition(bookPositionMs)
                    navController.popBackStack()
                },
                close = { navController.popBackStack() },
            )
        }

        settingsScreens(viewModel, navController)
    }
}

private fun NavGraphBuilder.settingsScreens(viewModel: BooksListViewModel, navController: NavHostController) {
    modal(NavigationItem.Settings.route) {
        SettingsScreen(
            booksViewModel = viewModel,
            onStopPlayer = viewModel::stopPlayer,
            openDiagnostics = { navController.navigate(NavigationItem.Diagnostics.route) },
            openLicenses = { navController.navigate(NavigationItem.Licenses.route) },
            openConverter = viewModel.converterEntry()?.let { { navController.navigate(NavigationItem.Converter.routeFor(null)) } },
        ) {
            navController.popBackStack()
        }
    }

    modal(NavigationItem.Diagnostics.route) {
        DiagnosticsScreen { navController.popBackStack() }
    }

    modal(NavigationItem.Licenses.route) {
        LicensesScreen { navController.popBackStack() }
    }

    composable(
        route = NavigationItem.Converter.route,
        arguments = listOf(
            navArgument("bookId") {
                type = NavType.StringType
                defaultValue = ""
            },
        ),
    ) {
        ConverterScreen { navController.popBackStack() }
    }
}

private fun NavGraphBuilder.libraryExtras(viewModel: BooksListViewModel, navController: NavHostController) {
    val playFromLibrary: (String) -> Unit = { bookId ->
        viewModel.requestPlay(bookId)
        navController.popBackStack(NavigationItem.BookList.route, inclusive = false)
    }

    modal(NavigationItem.Queue.route) {
        QueueScreen(onPlay = { playFromLibrary(it.id) }, close = { navController.popBackStack() })
    }

    modal(NavigationItem.Search.route) {
        SearchScreen(
            onPlay = { playFromLibrary(it.id) },
            onSearchLibriVox = { query -> navController.navigate(NavigationItem.LibriVox.routeFor(query)) },
            close = { navController.popBackStack() },
        )
    }

    composable(
        route = NavigationItem.Folders.route,
        arguments = FOLDER_ARGUMENTS.map { name ->
            navArgument(name) {
                type = NavType.StringType
                defaultValue = ""
            }
        },
    ) {
        FolderScreen(onPlay = { playFromLibrary(it.id) }, close = { navController.popBackStack() })
    }

    modal(NavigationItem.Statistics.route) {
        StatisticsScreen(
            onOpenBook = { bookId -> navController.navigate(NavigationItem.BookStatistics.routeFor(bookId)) },
            close = { navController.popBackStack() },
        )
    }

    modal(NavigationItem.BookStatistics.route) {
        val cameFromGlobal = navController.previousBackStackEntry?.destination?.route == NavigationItem.Statistics.route
        BookStatisticsScreen(
            onOpenAll = if (cameFromGlobal) null else ({ navController.navigate(NavigationItem.Statistics.route) }),
            close = { navController.popBackStack() },
        )
    }

    modal(NavigationItem.Text.route) {
        TextPaneScreen(close = { navController.popBackStack() })
    }

    modal(NavigationItem.Characters.route) {
        CharactersScreen(
            currentPositionMs = { viewModel.selectedBook.value?.bookPositionMs },
            onJump = { bookPositionMs ->
                viewModel.seekToBookPosition(bookPositionMs)
                navController.popBackStack()
            },
            close = { navController.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.libriVoxScreens(viewModel: BooksListViewModel, navController: NavHostController) {
    modal(NavigationItem.LibriVox.route, arguments = optionalStrings(LibriVoxSearchViewModel.QUERY_ARGUMENT)) {
        LibriVoxSearchScreen(
            onOpenBook = { book -> navController.navigate(NavigationItem.LibriVoxBookPage.routeFor(book)) },
            close = { navController.popBackStack() },
        )
    }

    modal(NavigationItem.LibriVoxBookPage.route, arguments = LIBRIVOX_BOOK_ARGUMENTS) {
        LibriVoxBookScreen(onListen = { bookId ->
            viewModel.requestPlay(bookId)
            navController.popBackStack(NavigationItem.BookList.route, inclusive = false)
        }, close = { navController.popBackStack() })
    }
}

class LibraryDestinations(
    val queue: () -> Unit = {},
    val search: () -> Unit = {},
    val librivox: () -> Unit = {},
    val folders: (FolderLocation) -> Unit = {},
    val statistics: () -> Unit = {},
    val bookStatistics: (bookId: String) -> Unit = {},
    val characters: (bookId: String) -> Unit = {},
    val text: (bookId: String) -> Unit = {},
)

private fun NavGraphBuilder.modal(route: String, arguments: List<NamedNavArgument> = emptyList(), screen: @Composable () -> Unit) {
    composable(
        route = route,
        arguments = arguments,
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

private fun optionalStrings(vararg names: String): List<NamedNavArgument> = names.map { name ->
    navArgument(name) {
        type = NavType.StringType
        defaultValue = ""
    }
}

private val LIBRIVOX_BOOK_ARGUMENTS = listOf(navArgument(LibriVoxBookViewModel.ID_ARGUMENT) { type = NavType.StringType }) +
    optionalStrings(LibriVoxBookViewModel.TITLE_ARGUMENT, LibriVoxBookViewModel.AUTHORS_ARGUMENT, LibriVoxBookViewModel.CREATOR_ARGUMENT)

private val FOLDER_ARGUMENTS = listOf(FolderViewModel.KIND_ARGUMENT, FolderViewModel.ROOT_ARGUMENT, FolderViewModel.PATH_ARGUMENT)
