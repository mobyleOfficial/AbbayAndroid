package com.mobyle.abbay.presentation.characters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.SnackbarResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.bookmarks.EmptyMessage
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.AbbayScreen
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.CharacterNote
import kotlinx.coroutines.launch

@Composable
fun CharactersScreen(
    viewModel: CharactersViewModel = hiltViewModel(),
    currentPositionMs: () -> Long?,
    onJump: (Long) -> Unit,
    close: () -> Unit,
) {
    val characters by viewModel.characters.collectAsState()
    var editing by remember { mutableStateOf<CharacterNote?>(null) }
    var editingPosition by remember { mutableStateOf<Long?>(null) }
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.characters_deleted)
    val undoLabel = stringResource(R.string.undo)

    editing?.let { character ->
        CharacterDialog(
            character = character,
            currentPositionMs = editingPosition,
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    AbbayScreen(
        title = stringResource(R.string.characters_title),
        onClose = close,
        scaffoldState = scaffoldState,
        actions = {
            IconButton(onClick = {
                editingPosition = currentPositionMs()
                editing = CharacterNote(0L, viewModel.bookId, "", "", null)
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.characters_add), tint = AbbayColors.content)
            }
        },
    ) {
        val list = characters ?: return@AbbayScreen
        if (list.isEmpty()) {
            EmptyMessage(stringResource(R.string.characters_empty))
            return@AbbayScreen
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { character ->
                CharacterRow(
                    character = character,
                    onEdit = {
                        editingPosition = currentPositionMs()
                        editing = character
                    },
                    onJump = character.firstPositionMs?.let { position -> { onJump(position) } },
                    onDelete = {
                        viewModel.delete(character)
                        scope.launch {
                            if (scaffoldState.snackbarHostState.showSnackbar(deletedMessage, undoLabel) == SnackbarResult.ActionPerformed) {
                                viewModel.restore(character)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun CharacterRow(character: CharacterNote, onEdit: () -> Unit, onJump: (() -> Unit)?, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClickLabel = stringResource(R.string.characters_edit), onClick = onEdit)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(character.name, style = MaterialTheme.typography.bodyLarge, color = AbbayColors.content)
            if (character.description.isNotBlank()) {
                Text(
                    character.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA)
                )
            }
            character.firstPositionMs?.let {
                Text(
                    stringResource(R.string.characters_first_at, it.toHHMMSS()),
                    style = MaterialTheme.typography.bodySmall,
                    color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
                )
            }
        }
        onJump?.let { jump ->
            IconButton(onClick = jump) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.characters_go_to, character.name),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = AbbayColors.content)
        }
    }
}

private const val SECONDARY_ALPHA = 0.75f
