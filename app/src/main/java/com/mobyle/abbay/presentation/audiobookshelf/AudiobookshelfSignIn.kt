package com.mobyle.abbay.presentation.audiobookshelf

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobyle.abbay.R
import com.mobyle.abbay.presentation.common.theme.AbbayColors
import com.mobyle.abbay.presentation.common.widgets.abbayTextFieldColors
import com.model.AudiobookshelfAccount
import com.model.AudiobookshelfSignInError

@Composable
internal fun SignInForm(account: AudiobookshelfAccount?, viewModel: AudiobookshelfViewModel) {
    val isSigningIn by viewModel.isSigningIn.collectAsState()
    val error by viewModel.signInError.collectAsState()
    var server by rememberSaveable { mutableStateOf(account?.serverUrl.orEmpty()) }
    var username by rememberSaveable { mutableStateOf(account?.username.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordShown by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val canSubmit = server.isNotBlank() && username.isNotBlank() && !isSigningIn
    val submit = {
        if (canSubmit) {
            focusManager.clearFocus()
            viewModel.signIn(server, username, password)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        SignInHeader(isSessionEnded = account != null)
        Spacer(Modifier.height(4.dp))
        FormField(
            value = server,
            onValueChange = { server = it },
            label = stringResource(R.string.audiobookshelf_server),
            placeholder = stringResource(R.string.audiobookshelf_server_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next, autoCorrectEnabled = false),
        )
        FormField(
            value = username,
            onValueChange = { username = it },
            label = stringResource(R.string.audiobookshelf_username),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, autoCorrectEnabled = false),
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            isShown = isPasswordShown,
            onToggleShown = { isPasswordShown = !isPasswordShown },
            onGo = submit,
        )
        error?.let { SignInErrorText(it) }
        SignInButton(isSigningIn = isSigningIn, isEnabled = canSubmit, onClick = submit)
    }
}

@Composable
private fun SignInHeader(isSessionEnded: Boolean) {
    Icon(Icons.Outlined.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(56.dp))
    Text(
        stringResource(R.string.audiobookshelf_sign_in_title),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = AbbayColors.content,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        stringResource(if (isSessionEnded) R.string.audiobookshelf_session_ended else R.string.audiobookshelf_sign_in_body),
        style = MaterialTheme.typography.bodyMedium,
        color = AbbayColors.content.copy(alpha = SECONDARY_ALPHA),
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = FORM_WIDTH),
    )
}

@Composable
private fun SignInErrorText(error: AudiobookshelfSignInError) {
    Text(
        stringResource(messageOf(error)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .widthIn(max = FORM_WIDTH)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, isShown: Boolean, onToggleShown: () -> Unit, onGo: () -> Unit) {
    FormField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.audiobookshelf_password),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        visualTransformation = if (isShown) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleShown) {
                Icon(
                    if (isShown) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (isShown) R.string.audiobookshelf_hide_password else R.string.audiobookshelf_show_password,
                    ),
                    tint = AbbayColors.content,
                )
            }
        },
    )
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    placeholder: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        colors = abbayTextFieldColors(),
        modifier = Modifier
            .widthIn(max = FORM_WIDTH)
            .fillMaxWidth(),
    )
}

@Composable
private fun SignInButton(isSigningIn: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.audiobookshelf_sign_in)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(top = 8.dp)
            .widthIn(max = FORM_WIDTH)
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = if (isEnabled || isSigningIn) 1f else DISABLED_ALPHA))
            .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
            .semantics { if (isSigningIn) contentDescription = label },
    ) {
        if (isSigningIn) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onTertiary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiary)
        }
    }
}

private fun messageOf(error: AudiobookshelfSignInError): Int = when (error) {
    AudiobookshelfSignInError.INVALID_ADDRESS -> R.string.audiobookshelf_error_address
    AudiobookshelfSignInError.UNREACHABLE -> R.string.audiobookshelf_error_unreachable
    AudiobookshelfSignInError.NOT_A_SERVER -> R.string.audiobookshelf_error_not_server
    AudiobookshelfSignInError.WRONG_CREDENTIALS -> R.string.audiobookshelf_error_credentials
}

private val FORM_WIDTH = 420.dp
private const val SECONDARY_ALPHA = 0.75f
private const val DISABLED_ALPHA = 0.4f
