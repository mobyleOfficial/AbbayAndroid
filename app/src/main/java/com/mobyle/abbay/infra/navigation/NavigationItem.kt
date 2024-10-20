package com.mobyle.abbay.infra.navigation

enum class Screen {
    BOOKS_LIST,
    FOLDER,
    SETTINGS
}

sealed class NavigationItem(val route: String) {
    data object BookList : NavigationItem(Screen.BOOKS_LIST.name)
    data object Settings : NavigationItem(Screen.SETTINGS.name)
    data object Folder : NavigationItem(Screen.FOLDER.name)
}