package com.mobyle.abbay.infra.cast

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.mobyle.abbay.presentation.cast.CastRouteButton
import com.mobyle.abbay.presentation.common.theme.AbbayColors

@Composable
fun CastButton(modifier: Modifier = Modifier) {
    val tint = AbbayColors.content.toArgb()
    AndroidView(factory = { context -> CastRouteButton.create(context, tint) ?: android.view.View(context) }, modifier = modifier)
}
