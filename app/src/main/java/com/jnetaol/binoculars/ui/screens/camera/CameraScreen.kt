package com.jnetaol.binoculars.ui.screens.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.media.ExifInterface
import android.os.Looper
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.jnetaol.binoculars.engine.CameraManager
import com.jnetaol.binoculars.engine.DistanceEstimator
import com.jnetaol.binoculars.engine.SettingsManager
import com.jnetaol.binoculars.logger.DebugLogger
import com.jnetaol.binoculars.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private fun convertToDMS(value: Double): String {
    val abs = Math.abs(value)
    val d = abs.toInt()
    val m = ((abs - d) * 60).toInt()
    val s = ((abs - d - m / 60.0) * 3600 * 100).toInt()
    return "$d/1,$m/1,$s/100"
}

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onPhotoCaptured: (File, Float) -> Unit,
    onCameraReady: ((com.jnetaol.binoculars.engine.CameraManager?) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val hasCamPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) { Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show(); onClose() }
    }

    LaunchedEffect(Unit) { if (!hasCamPerm) permLauncher.launch(Manifest.permission.CAMERA) }

    if (!hasCamPerm) {
        Box(Modifier.fillMaxSize().background(DarkBackground), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonGreen)
        }
        return
    }

    val cameraManager = remember { CameraManager(context, lifecycleOwner) }

    val showDistance by SettingsManager.showDistanceOverlay(context).collectAsState(initial = true)
    val nightVision by SettingsManager.nightVisionMode(context).collectAsState(initial = false)
    val showGrid by SettingsManager.showGrid(context).collectAsState(initial = true)
    val useScreenshotCapture by SettingsManager.useScreenshotCapture(context).collectAsState(initial = true)
    val saveLocation by SettingsManager.saveLocationMetadata(context).collectAsState(initial = false)
    val flashlightOn by SettingsManager.flashlightEnabled(context).collectAsState(initial = false)

    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(8f) }
    var isCapturing by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var previewRef by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(Unit) {
        cameraManager.onZoomChanged = { zoomRatio = it }
        onCameraReady?.invoke(cameraManager)
        onDispose {
            onCameraReady?.invoke(null!!)
            cameraManager.release()
        }
    }

    LaunchedEffect(Unit) { maxZoomRatio = cameraManager.getMaxZoomRatio() }

    LaunchedEffect(flashlightOn) {
        cameraManager.setFlashlight(flashlightOn)
    }

    LaunchedEffect(captureError) {
        captureError?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); captureError = null }
    }

    fun doCapture() {
        if (isCapturing) return
        isCapturing = true

        val fClient = try { LocationServices.getFusedLocationProviderClient(context) } catch (_: Exception) { null }

        if (useScreenshotCapture) {
            captureScreenshot(context, previewRef, zoomRatio, saveLocation, fClient) { file ->
                isCapturing = false
                if (file != null) onPhotoCaptured(file, zoomRatio)
                else captureError = "Capture failed. Please try again."
            }
        } else {
            cameraManager.capturePhoto { file ->
                if (file != null) {
                    if (saveLocation) embedLocation(context, file, fClient) {
                        isCapturing = false
                        onPhotoCaptured(it, zoomRatio)
                    }
                    else {
                        isCapturing = false
                        onPhotoCaptured(file, zoomRatio)
                    }
                } else {
                    captureScreenshot(context, previewRef, zoomRatio, saveLocation, fClient) { fallback ->
                        isCapturing = false
                        if (fallback != null) onPhotoCaptured(fallback, zoomRatio)
                        else captureError = "Capture failed. Please try again."
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    previewRef = this
                    post { cameraManager.startCamera(this) }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (nightVision) {
            Box(Modifier.fillMaxSize().background(Color(0x6600FF00)))
        }

        if (showDistance) DistanceOverlay(zoomRatio = zoomRatio)
        if (showGrid) ReticleOverlay(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        scope.launch {
                            val newVal = !flashlightOn
                            SettingsManager.setFlashlightEnabled(context, newVal)
                        }
                    }) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (flashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Flashlight",
                                tint = if (flashlightOn) AccentYellow else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (useScreenshotCapture) Icons.Default.Screenshot else Icons.Default.Camera,
                            contentDescription = "Capture mode",
                            tint = if (useScreenshotCapture) AccentYellow else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Volume keys to zoom", style = MaterialTheme.typography.labelSmall, color = TextTertiary.copy(alpha = 0.6f), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))

                ZoomSlider(currentZoom = zoomRatio, maxZoom = maxZoomRatio, onZoomChange = { cameraManager.setZoomRatio(it) })

                Spacer(Modifier.height(20.dp))

                ShutterButton(isCapturing = isCapturing, onClick = {
                    scope.launch { delay(50); doCapture() }
                })

                Spacer(Modifier.height(12.dp))

                Text("%.1fx".format(zoomRatio), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = NeonGreen, fontSize = 16.sp)
            }
        }
    }
}

private fun captureScreenshot(context: Context, view: PreviewView?, zoomRatio: Float, saveLocation: Boolean, locClient: FusedLocationProviderClient?, onResult: (File?) -> Unit) {
    val v = view ?: run { onResult(null); return }
    if (Looper.myLooper() != Looper.getMainLooper()) {
        android.os.Handler(Looper.getMainLooper()).post { captureScreenshot(context, view, zoomRatio, saveLocation, locClient, onResult) }
        return
    }
    try {
        val bitmap = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bitmap))
        val dir = File(context.cacheDir, "photos")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "BINOC_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        DebugLogger.i("CameraScreen", "Screenshot: ${file.name}")
        if (saveLocation && locClient != null) embedLocation(context, file, locClient) { onResult(it) }
        else onResult(file)
    } catch (e: Exception) {
        DebugLogger.e("CameraScreen", "Screenshot failed: ${e.message}", "CAM005", e)
        onResult(null)
    }
}

private fun embedLocation(context: Context, photoFile: File, locClient: FusedLocationProviderClient?, onResult: (File) -> Unit) {
    val client = locClient ?: run { onResult(photoFile); return }
    try {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) try {
                    val exif = ExifInterface(photoFile.absolutePath)
                    val lat = loc.latitude; val lon = loc.longitude
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertToDMS(lat))
                    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (lat >= 0) "N" else "S")
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertToDMS(lon))
                    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (lon >= 0) "E" else "W")
                    if (loc.hasAltitude()) {
                        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, (loc.altitude * 100).toLong().toString())
                        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, if (loc.altitude >= 0) "0" else "1")
                    }
                    exif.saveAttributes()
                } catch (_: Exception) {}
                onResult(photoFile)
            }
            .addOnFailureListener { onResult(photoFile) }
    } catch (_: Exception) { onResult(photoFile) }
}

@Composable
private fun DistanceOverlay(zoomRatio: Float) {
    val result = remember(zoomRatio) { DistanceEstimator.estimateDistance(zoomRatio) }
    Box(Modifier.fillMaxSize().padding(top = 16.dp), contentAlignment = Alignment.TopCenter) {
        Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DISTANCE ESTIMATOR", style = MaterialTheme.typography.labelSmall, color = NeonGreen.copy(alpha = 0.7f), fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(4.dp))
                Text(result.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NeonGreen, fontSize = 18.sp)
                Spacer(Modifier.height(2.dp))
                Text("Target: ~${"%.1f".format(result.estimatedDistanceMeters)}m | Confidence: ${"%.0f".format(result.confidencePercent)}%", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ReticleOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.7f).align(Alignment.Center).border(1.dp, ReticleColor.copy(alpha = 0.3f)))
        Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.Center).background(ReticleColor.copy(alpha = 0.15f)))
        Box(Modifier.fillMaxHeight().width(1.dp).align(Alignment.Center).background(ReticleColor.copy(alpha = 0.15f)))
        Box(Modifier.size(6.dp).align(Alignment.Center).clip(CircleShape).background(ReticleColor.copy(alpha = 0.5f)))
    }
}

@Composable
private fun ZoomSlider(currentZoom: Float, maxZoom: Float, onZoomChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth(0.85f), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Slider(value = currentZoom, onValueChange = onZoomChange, valueRange = 1f..maxZoom, modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen, inactiveTrackColor = NeonGreen.copy(alpha = 0.2f)))
            Icon(Icons.Default.ZoomIn, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
            Text("%.0fx".format(maxZoom), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
    }
}

@Composable
private fun ShutterButton(isCapturing: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(72.dp).clip(CircleShape).border(4.dp, ShutterColor, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(if (isCapturing) NeonGreen.copy(alpha = 0.6f) else ShutterColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = rememberRipple(bounded = true, radius = 29.dp), onClick = onClick),
            contentAlignment = Alignment.Center) {
            if (isCapturing) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        }
    }
}
