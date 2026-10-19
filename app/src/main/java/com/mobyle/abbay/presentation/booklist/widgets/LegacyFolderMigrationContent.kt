package com.mobyle.abbay.presentation.booklist.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayTextStyles
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton

/** One-time screen after the update from the storage-permission version. */
@Composable
fun LegacyFolderMigrationContent(onChooseFolder: () -> Unit, onSkip: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 24.dp)
    ) {
        Text(
            stringResource(R.string.legacy_migration_title),
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        )
        Image(
            painter = painterResource(R.drawable.img_no_permissions),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .padding(vertical = 16.dp)
        )
        Text(
            stringResource(R.string.legacy_migration_body),
            style = AbbayTextStyles.subtitleText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onChooseFolder,
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(
                stringResource(R.string.legacy_migration_choose),
                style = AbbayTextStyles.buttonTextLarge.copy(color = MaterialTheme.colorScheme.primary)
            )
        }
        AbbayTextButton(onClick = onSkip) {
            Text(stringResource(R.string.legacy_migration_skip), style = AbbayTextStyles.subtitleText)
        }
    }
}
