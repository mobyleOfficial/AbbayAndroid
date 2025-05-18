package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource

@Composable
fun SVGIcon(
    modifier: Modifier = Modifier,
    path: Int,
    description: String = "",
    color: Color = Color.White,
) = Icon(
    imageVector = ImageVector.vectorResource(id = path),
    contentDescription = description,
    tint = color,
    modifier = modifier
)