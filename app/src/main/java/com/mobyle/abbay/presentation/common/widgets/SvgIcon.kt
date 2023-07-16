package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource

@Composable
fun SVGIcon(path: Int, description: String = "") = Icon(
    imageVector = ImageVector.vectorResource(id = path),
    contentDescription = description
)