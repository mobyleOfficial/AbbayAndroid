package com.mobyle.abbay.presentation.booklist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MiniPlayer(modifier: Modifier) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .then(modifier)) {
        Text("Image")
        Column {
            Text("name")
            Column {
                Row {
                    Text("Ouvido")
                    Text("Faltando")
                }
            }
            Row {
                Text("Controles")
                Text("Arquivos")
            }
        }
    }
}