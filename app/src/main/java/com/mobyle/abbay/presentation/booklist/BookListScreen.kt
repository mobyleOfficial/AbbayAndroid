package com.mobyle.abbay.presentation.booklist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@ExperimentalPermissionsApi
@Composable
fun BookListScreen() {
    val permissionState =
        rememberPermissionState(permission = Manifest.permission.READ_EXTERNAL_STORAGE) {
            if (it) {
                Log.d("HelpMe", "NO HA PERMISSIONE")
            } else {
                Log.d("HelpMe", "NO HA PERMISSIONE")
            }
        }


    val state = rememberPermissionState(Manifest.permission.CAMERA)

    when {
        state.status.isGranted -> Text("HABEMOS PERMISSAO")
        else -> {
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}

fun performFileSearch(activity: Activity) {
    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        startActivityForResult(activity, this, 0, null)
    }
}

@ExperimentalPermissionsApi
@Composable
@Preview
fun BookListScreenPreview() {
    BookListScreen()
}