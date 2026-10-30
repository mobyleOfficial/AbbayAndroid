package com.mobyle.abbay.infra.navigation

import android.net.Uri
import com.library.FolderLocation
import com.mobyle.abbay.presentation.folders.FolderViewModel
import com.mobyle.abbay.presentation.librivox.LibriVoxBookViewModel
import com.mobyle.abbay.presentation.librivox.LibriVoxSearchViewModel
import com.model.LibriVoxBook

enum class Screen {
    BOOKS_LIST,
    SETTINGS,
    BOOK_DETAILS,
    BOOKMARKS,
    HISTORY,
    QUEUE,
    SEARCH,
    FOLDERS,
    STATISTICS,
    BOOK_STATISTICS,
    CHARACTERS,
    TEXT,
    DIAGNOSTICS,
    LICENSES,
    CONVERTER,
    LIBRIVOX,
    LIBRIVOX_BOOK,
}

sealed class NavigationItem(val route: String) {
    data object BookList : NavigationItem(Screen.BOOKS_LIST.name)
    data object Settings : NavigationItem(Screen.SETTINGS.name)
    data object BookDetails : NavigationItem("${Screen.BOOK_DETAILS.name}/{bookId}") {
        fun routeFor(bookId: String) = "${Screen.BOOK_DETAILS.name}/$bookId"
    }
    data object Bookmarks : NavigationItem("${Screen.BOOKMARKS.name}/{bookId}") {
        fun routeFor(bookId: String) = "${Screen.BOOKMARKS.name}/$bookId"
    }
    data object History : NavigationItem("${Screen.HISTORY.name}/{bookId}") {
        fun routeFor(bookId: String) = "${Screen.HISTORY.name}/$bookId"
    }
    data object Queue : NavigationItem(Screen.QUEUE.name)
    data object Diagnostics : NavigationItem(Screen.DIAGNOSTICS.name)
    data object Licenses : NavigationItem(Screen.LICENSES.name)
    data object Converter : NavigationItem("${Screen.CONVERTER.name}?bookId={bookId}") {
        fun routeFor(bookId: String?) = "${Screen.CONVERTER.name}?bookId=${bookId.orEmpty()}"
    }
    data object Search : NavigationItem(Screen.SEARCH.name)
    data object LibriVox : NavigationItem(
        "${Screen.LIBRIVOX.name}?${LibriVoxSearchViewModel.QUERY_ARGUMENT}={${LibriVoxSearchViewModel.QUERY_ARGUMENT}}",
    ) {
        fun routeFor(query: String = "") = "${Screen.LIBRIVOX.name}?${LibriVoxSearchViewModel.QUERY_ARGUMENT}=${Uri.encode(query)}"
    }
    data object LibriVoxBookPage : NavigationItem(
        "${Screen.LIBRIVOX_BOOK.name}/{${LibriVoxBookViewModel.ID_ARGUMENT}}" +
            "?${LibriVoxBookViewModel.TITLE_ARGUMENT}={${LibriVoxBookViewModel.TITLE_ARGUMENT}}" +
            "&${LibriVoxBookViewModel.AUTHORS_ARGUMENT}={${LibriVoxBookViewModel.AUTHORS_ARGUMENT}}" +
            "&${LibriVoxBookViewModel.CREATOR_ARGUMENT}={${LibriVoxBookViewModel.CREATOR_ARGUMENT}}",
    ) {
        fun routeFor(book: LibriVoxBook) = "${Screen.LIBRIVOX_BOOK.name}/${Uri.encode(book.id)}" +
            "?${LibriVoxBookViewModel.TITLE_ARGUMENT}=${Uri.encode(book.title)}" +
            "&${LibriVoxBookViewModel.AUTHORS_ARGUMENT}=${Uri.encode(book.authors)}" +
            "&${LibriVoxBookViewModel.CREATOR_ARGUMENT}=${Uri.encode(book.creator.orEmpty())}"
    }
    data object Folders : NavigationItem(
        "${Screen.FOLDERS.name}?${FolderViewModel.KIND_ARGUMENT}={${FolderViewModel.KIND_ARGUMENT}}" +
            "&${FolderViewModel.ROOT_ARGUMENT}={${FolderViewModel.ROOT_ARGUMENT}}" +
            "&${FolderViewModel.PATH_ARGUMENT}={${FolderViewModel.PATH_ARGUMENT}}",
    ) {
        fun routeFor(location: FolderLocation): String {
            val kind = when {
                location.isSingleFiles -> FolderViewModel.KIND_SINGLE_FILES
                location.isTop -> FolderViewModel.KIND_TOP
                else -> FolderViewModel.KIND_FOLDER
            }
            return "${Screen.FOLDERS.name}?${FolderViewModel.KIND_ARGUMENT}=$kind" +
                "&${FolderViewModel.ROOT_ARGUMENT}=${location.rootId ?: ""}" +
                "&${FolderViewModel.PATH_ARGUMENT}=${Uri.encode(location.path)}"
        }
    }
    data object Statistics : NavigationItem(Screen.STATISTICS.name)
    data object BookStatistics : NavigationItem("${Screen.BOOK_STATISTICS.name}/{bookId}") {
        fun routeFor(bookId: String) = "${Screen.BOOK_STATISTICS.name}/$bookId"
    }
    data object Text : NavigationItem("${Screen.TEXT.name}/{bookId}") {
        fun routeFor(bookId: String) = "${Screen.TEXT.name}/$bookId"
    }
    data object Characters : NavigationItem("${Screen.CHARACTERS.name}/{bookId}") {
        fun routeFor(bookId: String) = "${Screen.CHARACTERS.name}/$bookId"
    }
}
