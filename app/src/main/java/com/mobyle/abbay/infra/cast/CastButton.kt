package com.mobyle.abbay.infra.cast

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.mobyle.abbay.presentation.cast.CastRouteButton
import com.mobyle.abbay.presentation.common.theme.AbbayColors

/**
 * The Cast route button for the player's top bar. It lives in `infra`, not in presentation, because it is a
 * view of `:cast`.
 */
@Composable
fun CastButton(modifier: Modifier = Modifier) {
    val tint = AbbayColors.content.toArgb()
    // No fixed size: the view is gone (and takes no room) while no Cast device is found.
    AndroidView(factory = { context -> CastRouteButton.create(context, tint) ?: android.view.View(context) }, modifier = modifier)
}
