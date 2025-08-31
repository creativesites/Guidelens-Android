package com.craftflowtechnologies.guidelens.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestCameraPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )

    val context = LocalContext.current

    LaunchedEffect(key1 = cameraPermissionState.status) {
        when {
            cameraPermissionState.status.isGranted -> {
                onPermissionGranted()
            }
            cameraPermissionState.status.shouldShowRationale -> {
                // Show rationale to user
                onPermissionDenied()
            }
            else -> {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestAudioPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val audioPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    LaunchedEffect(key1 = audioPermissionState.status) {
        when {
            audioPermissionState.status.isGranted -> {
                onPermissionGranted()
            }
            audioPermissionState.status.shouldShowRationale -> {
                onPermissionDenied()
            }
            else -> {
                audioPermissionState.launchPermissionRequest()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestVoiceAndVideoPermissions(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: (List<String>) -> Unit
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(cameraPermissionState.status, audioPermissionState.status) {
        val deniedPermissions = mutableListOf<String>()
        
        if (!cameraPermissionState.status.isGranted) {
            deniedPermissions.add("Camera")
        }
        if (!audioPermissionState.status.isGranted) {
            deniedPermissions.add("Microphone")
        }

        when {
            deniedPermissions.isEmpty() -> {
                onPermissionsGranted()
            }
            cameraPermissionState.status.shouldShowRationale || 
            audioPermissionState.status.shouldShowRationale -> {
                onPermissionsDenied(deniedPermissions)
            }
            else -> {
                if (!cameraPermissionState.status.isGranted) {
                    cameraPermissionState.launchPermissionRequest()
                }
                if (!audioPermissionState.status.isGranted) {
                    audioPermissionState.launchPermissionRequest()
                }
            }
        }
    }
}