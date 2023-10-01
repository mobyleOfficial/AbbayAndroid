package com.mobyle.abbay.presentation.common.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@ExperimentalMaterialApi
@Composable
fun AbbayPlayer(state: BottomSheetScaffoldState) {
    BottomSheetScaffold(
        scaffoldState = state,
        sheetContent = {
            Text("Collapsed")
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Expanded")
        }
    }
}