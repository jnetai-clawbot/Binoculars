package com.jnetaol.binoculars.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jnetaol.binoculars.engine.CameraManager
import com.jnetaol.binoculars.logger.DebugLogger
import com.jnetaol.binoculars.ui.theme.*
import java.io.File

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onPhotoCaptured: (File, Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) {
        Box(
            modifier = Modifier.fillMaxSize().background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NeonGreen)
        }
        return
    }

    val cameraManager = remember { CameraManager(context, lifecycleOwner) }

    var zoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(8f) }
    var isCapturing by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        cameraManager.onZoomChanged = { zoom ->
            zoomRatio = zoom
        }
        onDispose { cameraManager.release() }
    }

    LaunchedEffect(Unit) {
        maxZoomRatio = cameraManager.getMaxZoomRatio()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    post {
                        cameraManager.startCamera(this)
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (showGrid) {
            ReticleOverlay(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showGrid = !showGrid }) {
                        Icon(
                            imageVector = if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = "Toggle grid",
                            tint = if (showGrid) NeonGreen else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    FlashIndicator()
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ZoomSlider(
                    currentZoom = zoomRatio,
                    maxZoom = maxZoomRatio,
                    onZoomChange = { cameraManager.setZoomRatio(it) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                ShutterButton(
                    isCapturing = isCapturing,
                    onClick = {
                        if (!isCapturing) {
                            isCapturing = true
                            cameraManager.capturePhoto { file ->
                                isCapturing = false
                                if (file != null) {
                                    onPhotoCaptured(file, zoomRatio)
                                } else {
                                    DebugLogger.e(
                                        "CameraScreen",
                                        "Photo capture failed",
                                        "CAM004"
                                    )
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "%.1fx".format(zoomRatio),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ReticleOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .align(Alignment.Center)
                .border(1.dp, ReticleColor.copy(alpha = 0.3f))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .background(ReticleColor.copy(alpha = 0.15f))
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .align(Alignment.Center)
                .background(ReticleColor.copy(alpha = 0.15f))
        )

        Box(
            modifier = Modifier
                .size(6.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(ReticleColor.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun FlashIndicator() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FlashOff,
            contentDescription = "Flash off",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ZoomSlider(
    currentZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(0.85f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )

            Slider(
                value = currentZoom,
                onValueChange = onZoomChange,
                valueRange = 1f..maxZoom,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = NeonGreen,
                    activeTrackColor = NeonGreen,
                    inactiveTrackColor = NeonGreen.copy(alpha = 0.2f)
                )
            )

            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = "%.0fx".format(maxZoom),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun ShutterButton(
    isCapturing: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(4.dp, ShutterColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(if (isCapturing) NeonGreen.copy(alpha = 0.6f) else ShutterColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = 29.dp),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
