package com.mobyle.abbay.presentation.booklist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@ExperimentalPermissionsApi
@Composable
fun PermissionsHandlerScreen() {
    var shouldShowRationaleBefore by remember {
        mutableStateOf(false)
    }

    var hasShowRationaleBefore by remember {
        mutableStateOf(false)
    }
    val permissionState =
        rememberPermissionState(permission = Manifest.permission.READ_EXTERNAL_STORAGE) { isGranted ->
            if (isGranted) {
                Log.d(BookListScreenCompanionClass.TAG, "Select folder")
            } else {
                if (!shouldShowRationaleBefore && hasShowRationaleBefore) {
                    shouldShowRationaleBefore = true
                }

                hasShowRationaleBefore = true
            }
        }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner, effect = {
        val eventObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    permissionState.launchPermissionRequest()
                }

                else -> {
                    // Do nothing in other states
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(eventObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(eventObserver)
        }
    })

    val state = permissionState.status

    SideEffect {
        val permissionCheckResult =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            Log.d(BookListScreenCompanionClass.TAG, "Select folder")
        } else {
            if (!hasShowRationaleBefore) {
                permissionState.launchPermissionRequest()
            }
        }
    }

    when {
        state.isGranted -> Text("Select folder")
        state.shouldShowRationale -> Text("Show Empty State")
        else -> {
            if (shouldShowRationaleBefore) {
                Text("Clicked outside, Show empty state")
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

//todo: Open folder selector intent
fun performFileSearch(activity: Activity) {
    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        startActivityForResult(activity, this, 0, null)
    }
}

@ExperimentalPermissionsApi
@Composable
@Preview
fun PermissionsHandlerScreenPreview() {
    PermissionsHandlerScreen()
}

private class BookListScreenCompanionClass {
    companion object {
        const val TAG = "HelpMe"
    }
}