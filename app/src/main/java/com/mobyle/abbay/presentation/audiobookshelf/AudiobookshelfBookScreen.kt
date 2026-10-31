package com.mobyle.abbay.presentation.audiobookshelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.librivox.CatalogBookScreen
import com.model.CatalogSeries

@Composable
fun AudiobookshelfBookScreen(
    viewModel: AudiobookshelfBookViewModel = hiltViewModel(),
    onListen: (bookId: String) -> Unit,
    onOpenSeries: (CatalogSeries) -> Unit,
    close: () -> Unit,
) {
    val account by viewModel.account.collectAsState()
    CatalogBookScreen(
        viewModel = viewModel,
        title = stringResource(R.string.audiobookshelf_title),
        credit = stringResource(R.string.audiobookshelf_credit, account?.serverUrl?.let(::hostOf).orEmpty()),
        onListen = onListen,
        close = close,
        onOpenSeries = onOpenSeries,
    )
}
