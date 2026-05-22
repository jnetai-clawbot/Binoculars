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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jnetaol.binoculars.engine.CameraManager
import com.jnetaol.binoculars.engine.DistanceEstimator
import com.jnetaol.binoculars.engine.SettingsManager
import com.jnetaol.binoculars.logger.DebugLogger
import com.jnetaol.binoculars.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onPhotoCaptured: (File, Float) -> Unit,
    onCameraReady: ((com.jnetaol.binoculars.engine.CameraManager?) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

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

    val showDistance by SettingsManager.showDistanceOverlay(context).collectAsState(initial = true)
    val nightVision by SettingsManager.nightVisionMode(context).collectAsState(initial = false)
    val showGrid by SettingsManager.showGrid(context).collectAsState(initial = true)
    val flashlightOn by SettingsManager.flashlightEnabled(context).collectAsState(initial = false)

    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(8f) }
    var isCapturing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        cameraManager.onZoomChanged = { zoom ->
            zoomRatio = zoom
        }
        onCameraReady?.invoke(cameraManager)
        onDispose {
            onCameraReady?.invoke(null!!)
            cameraManager.release()
        }
    }

    LaunchedEffect(Unit) {
        maxZoomRatio = cameraManager.getMaxZoomRatio()
    }

    LaunchedEffect(flashlightOn) {
        try {
            cameraManager.setFlashlight(flashlightOn)
        } catch (_: Exception) {}
    }

    LaunchedEffect(captureError) {
        captureError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            captureError = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
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

        if (nightVision) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x6600FF00))
            )
        }

        if (showDistance) {
            DistanceOverlay(zoomRatio = zoomRatio)
        }

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
                    IconButton(onClick = {
                        scope.launch {
                            val newVal = !flashlightOn
                            SettingsManager.setFlashlightEnabled(context, newVal)
                        }
                    }) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (flashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Flashlight",
                                tint = if (flashlightOn) AccentYellow else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Volume keys to zoom",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                                    captureError = "Photo capture failed. Please try again."
                                    DebugLogger.e("CameraScreen", "Photo capture failed", "CAM004")
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
private fun DistanceOverlay(zoomRatio: Float) {
    val distanceResult = remember(zoomRatio) {
        DistanceEstimator.estimateDistance(zoomRatio)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DISTANCE ESTIMATOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGreen.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = distanceResult.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Target: ~${"%.1f".format(distanceResult.estimatedDistanceMeters)}m | Confidence: ${"%.0f".format(distanceResult.confidencePercent)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
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
