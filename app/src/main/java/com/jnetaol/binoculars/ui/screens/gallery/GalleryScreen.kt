package com.jnetaol.binoculars.ui.screens.gallery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.ExifInterface
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jnetaol.binoculars.data.model.CapturedPhoto
import com.jnetaol.binoculars.ui.components.*
import com.jnetaol.binoculars.ui.screens.AppViewModel
import com.jnetaol.binoculars.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class GpsInfo(val lat: Double?, val lon: Double?, val alt: Double?)

private fun readGpsInfo(file: File): GpsInfo {
    return try {
        val exif = ExifInterface(file.absolutePath)
        val ll = FloatArray(2)
        val hasLoc = exif.getLatLong(ll)
        val alt = exif.getAltitude(0.0)
        if (hasLoc && ll[0] != 0f && ll[1] != 0f) GpsInfo(ll[0].toDouble(), ll[1].toDouble(), if (alt > 0.001) alt else null)
        else GpsInfo(null, null, null)
    } catch (_: Exception) { GpsInfo(null, null, null) }
}

@Composable
fun GalleryScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val photos by viewModel.allPhotos.collectAsState()
    var selectedPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    Box(Modifier.fillMaxSize().background(DarkBackground)) {
        if (selectedPhoto != null) {
            PhotoDetailScreen(photo = selectedPhoto!!, onClose = { selectedPhoto = null },
                onDelete = { showDeleteConfirm = true },
                onMoveToDcim = { viewModel.moveToDcim(listOf(selectedPhoto!!)); Toast.makeText(context, "Moved to DCIM/Binoculars", Toast.LENGTH_SHORT).show() })
            if (showDeleteConfirm) ConfirmDialog(
                title = "Delete Photo", message = "This action cannot be undone.",
                onConfirm = { viewModel.deletePhoto(selectedPhoto!!); selectedPhoto = null; showDeleteConfirm = false },
                onDismiss = { showDeleteConfirm = false })
            return
        }

        Column(Modifier.fillMaxSize()) {
            Surface(color = DarkSurface, shadowElevation = 4.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
                    Text("Gallery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                    if (photos.isNotEmpty()) {
                        IconButton(onClick = { selectionMode = !selectionMode; selectedIds = emptySet() }) {
                            Icon(if (selectionMode) Icons.Default.CheckCircle else Icons.Default.CheckBoxOutlineBlank, "Select", tint = if (selectionMode) NeonGreen else TextSecondary)
                        }
                    }
                    if (photos.isNotEmpty()) IconButton(onClick = { showClearAllConfirm = true }) { Icon(Icons.Default.DeleteSweep, "Delete all", tint = AccentRed.copy(alpha = 0.8f)) }
                }
            }

            if (selectionMode && selectedIds.isNotEmpty()) {
                Surface(color = NeonGreen.copy(alpha = 0.1f)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = {
                            val selected = photos.filter { it.id in selectedIds }
                            viewModel.moveToDcim(selected)
                            selectionMode = false; selectedIds = emptySet()
                        }) { Icon(Icons.Default.Folder, null, tint = NeonGreen, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Move to DCIM", color = NeonGreen, fontSize = 13.sp) }
                        TextButton(onClick = { selectedIds = photos.map { it.id }.toSet() }) { Icon(Icons.Default.SelectAll, null, tint = NeonTeal, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Select All", color = NeonTeal, fontSize = 13.sp) }
                    }
                }
            }

            if (photos.isEmpty()) EmptyState(icon = Icons.Default.PhotoLibrary, title = "Gallery is empty", subtitle = "Photos you capture will appear here")
            else LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(photos, key = { it.id }) { photo ->
                    GalleryThumbnail(photo, photo.id in selectedIds, selectionMode, onClick = {
                        if (selectionMode) selectedIds = if (photo.id in selectedIds) selectedIds - photo.id else selectedIds + photo.id
                        else selectedPhoto = photo
                    })
                }
            }
        }
    }
    if (showClearAllConfirm) ConfirmDialog(
        title = "Delete All Photos", message = "This will permanently delete all ${photos.size} photos.", confirmText = "Delete All",
        onConfirm = { viewModel.deleteAllPhotos(); showClearAllConfirm = false }, onDismiss = { showClearAllConfirm = false })
}

@Composable
private fun GalleryThumbnail(photo: CapturedPhoto, selected: Boolean, selectionMode: Boolean, onClick: () -> Unit) {
    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(DarkSurfaceVariant).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        val f = File(photo.filePath)
        if (f.exists()) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(f).crossfade(true).build(), contentDescription = "Photo ${photo.id}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Default.BrokenImage, "Missing", tint = TextTertiary, modifier = Modifier.size(32.dp))
        Box(Modifier.align(Alignment.BottomEnd).padding(4.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 4.dp, vertical = 2.dp)) { Text("%.1fx".format(photo.zoomLevel), color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        if (selectionMode && selected) Box(Modifier.fillMaxSize().background(NeonGreen.copy(alpha = 0.2f)), contentAlignment = Alignment.TopEnd) {
            Box(Modifier.padding(6.dp).size(22.dp).clip(CircleShape).background(NeonGreen), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = TextOnAccent, modifier = Modifier.size(14.dp)) }
        }
    }
}

@Composable
private fun PhotoDetailScreen(photo: CapturedPhoto, onClose: () -> Unit, onDelete: () -> Unit, onMoveToDcim: () -> Unit) {
    val context = LocalContext.current
    val df = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }
    val file = File(photo.filePath)
    val gps = remember(file) { if (file.exists()) readGpsInfo(file) else GpsInfo(null, null, null) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (file.exists()) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(file).crossfade(true).build(), contentDescription = "Photo ${photo.id}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.BrokenImage, null, tint = TextTertiary, modifier = Modifier.size(64.dp)); Spacer(Modifier.height(12.dp)); Text("Image not found", color = TextSecondary) }
        }

        Row(Modifier.fillMaxWidth().padding(12.dp).statusBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onClose, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Row {
                IconButton(onClick = onMoveToDcim, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) { Icon(Icons.Default.Folder, "Move to DCIM", tint = NeonGreen) }
                IconButton(onClick = onDelete, modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))) { Icon(Icons.Default.Delete, "Delete", tint = AccentRed) }
            }
        }

        if (gps.lat != null && gps.lon != null) {
            val txt = "%.6f, %.6f%s".format(gps.lat, gps.lon, if (gps.alt != null) " (%.1fm alt)".format(gps.alt) else "")
            Surface(Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 8.dp), color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = AccentBlue, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                    Text(txt, color = TextPrimary, fontSize = 11.sp, maxLines = 1); Spacer(Modifier.width(6.dp))
                    IconButton(onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("GPS", txt))
                        Toast.makeText(context, "GPS coordinates copied", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.size(22.dp)) { Icon(Icons.Default.ContentCopy, "Copy GPS", tint = NeonGreen.copy(alpha = 0.7f), modifier = Modifier.size(12.dp)) }
                }
            }
        }

        Surface(Modifier.align(Alignment.BottomCenter), color = Color.Black.copy(alpha = 0.7f)) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoChip(Icons.Default.ZoomIn, "%.1fx zoom".format(photo.zoomLevel))
                    InfoChip(Icons.Default.Image, formatFileSize(file.length()))
                    if (gps.lat != null) InfoChip(Icons.Default.LocationOn, "GPS tagged")
                }
                Spacer(Modifier.height(8.dp))
                Text(df.format(Date(photo.timestamp)), style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                Text(photo.filePath.substringAfterLast("/"), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = NeonGreen, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
