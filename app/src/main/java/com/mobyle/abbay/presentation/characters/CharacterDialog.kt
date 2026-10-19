package com.mobyle.abbay.presentation.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.widgets.AbbayAlertDialog
import com.mobyle.abbay.presentation.common.widgets.AbbayTextButton
import com.mobyle.abbay.presentation.utils.toHHMMSS
import com.model.CharacterNote

/**
 * Adds or edits a character. "First appears here" stores the player's position; a new character has it checked, so
 * adding one while listening is a name and one tap.
 */
@Composable
fun CharacterDialog(character: CharacterNote, currentPositionMs: Long?, onSave: (CharacterNote) -> Unit, onDismiss: () -> Unit) {
    val isNew = character.id == 0L
    var name by rememberSaveable { mutableStateOf(character.name) }
    var description by rememberSaveable { mutableStateOf(character.description) }
    val position = if (isNew) currentPositionMs else character.firstPositionMs ?: currentPositionMs
    var usePosition by rememberSaveable { mutableStateOf(if (isNew) currentPositionMs != null else character.firstPositionMs != null) }

    AbbayAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.characters_add else R.string.characters_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(CharacterNote.MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.characters_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(CharacterNote.MAX_DESCRIPTION_LENGTH) },
                    label = { Text(stringResource(R.string.characters_description)) },
                    modifier = Modifier.heightIn(min = 96.dp),
                )
                position?.let { positionMs ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(value = usePosition, role = Role.Checkbox, onValueChange = { usePosition = it }),
                    ) {
                        Checkbox(checked = usePosition, onCheckedChange = null)
                        Text(stringResource(R.string.characters_first_here, positionMs.toHHMMSS()))
                    }
                }
            }
        },
        confirmButton = {
            AbbayTextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(character.copy(name = name, description = description, firstPositionMs = position.takeIf { usePosition }))
                },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { AbbayTextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
