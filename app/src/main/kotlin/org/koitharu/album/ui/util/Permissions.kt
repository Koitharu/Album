package org.koitharu.album.ui.util

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberPermissionCheck(permission: String): State<Boolean> {
    val context = LocalContext.current
    val isGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(RequestPermission()) { result ->
        isGranted.value = result
    }
    LaunchedEffect(Unit) {
        if (!isGranted.value) {
            launcher.launch(permission)
        }
    }
    return isGranted
}

@Composable
fun rememberPermissionsCheck(vararg permissions: String): State<Boolean> {
    val context = LocalContext.current
    var missingPermissions by remember {
        mutableStateOf(
            permissions.filterNot { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val isGranted = remember(missingPermissions) {
        mutableStateOf(missingPermissions.isEmpty())
    }
    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { result ->
        missingPermissions = missingPermissions.filterNot { permission ->
            result.getOrDefault(permission, false)
        }
    }
    LaunchedEffect(Unit) {
        if (missingPermissions.isNotEmpty()) {
            launcher.launch(missingPermissions.toTypedArray())
        }
    }
    return isGranted
}