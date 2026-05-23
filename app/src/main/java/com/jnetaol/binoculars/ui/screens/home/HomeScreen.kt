package com.jnetaol.binoculars.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.binoculars.ui.components.*
import com.jnetaol.binoculars.ui.screens.AppViewModel
import com.jnetaol.binoculars.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val photoCount by viewModel.photoCount.collectAsState()
    val recentPhotos by viewModel.allPhotos.collectAsState()
    var showReadyStatus by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { delay(1500); showReadyStatus = false }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Binoculars", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = NeonGreen)
                    Text("Digital Zoom Camera", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings", tint = TextSecondary, modifier = Modifier.size(24.dp)) }
            }
        }

        item {
            AnimatedVisibility(visible = showReadyStatus && hasPermission, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = NeonGreen.copy(alpha = 0.1f)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("App capture ready", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            if (showReadyStatus && !hasPermission) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = AccentYellow.copy(alpha = 0.1f)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = AccentYellow, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Camera permission required", color = AccentYellow, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = onRequestPermission) { Text("Grant", color = AccentYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        item {
            NeonCard(borderColor = NeonGreen.copy(alpha = 0.3f)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RemoveRedEye, null, tint = NeonGreen, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("See Farther, Capture Closer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text("Use digital zoom to magnify distant objects and capture high-resolution photos.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(20.dp))
                    if (hasPermission) GlowButton("Open Viewfinder", Icons.Default.Camera, onOpenCamera, Modifier.fillMaxWidth(), NeonGreen, TextOnAccent)
                    else GlowButton("Grant Camera Permission", Icons.Default.Lock, onRequestPermission, Modifier.fillMaxWidth(), AccentYellow, TextOnAccent)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Photos", "$photoCount", Icons.Default.PhotoLibrary, NeonGreen, Modifier.weight(1f))
                StatCard("Gallery", "View", Icons.Default.Collections, NeonTeal, Modifier.weight(1f))
            }
        }

        item { SectionHeader("Quick Actions", NeonGreen) }

        item {
            NeonCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenGallery, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTeal),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NeonTeal.copy(alpha = 0.5f)))) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("View Gallery", fontSize = 13.sp)
                    }
                    val all = recentPhotos
                    if (all.isNotEmpty()) {
                        OutlinedButton(onClick = { viewModel.moveToDcim(all) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentYellow),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AccentYellow.copy(alpha = 0.5f)))) {
                            Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Move to DCIM", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item { SectionHeader("Recent Captures", NeonTeal) }

        if (recentPhotos.isEmpty()) {
            item { EmptyState(Icons.Default.CameraAlt, "No photos yet", "Open the viewfinder to start capturing zoomed photos") }
        } else {
            items(recentPhotos.size.coerceAtMost(6)) { index ->
                val photo = recentPhotos[index]
                NeonCard(borderColor = ClearColor) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, tint = NeonGreenDim, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(photo.filePath.substringAfterLast("/"), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
                            Text("${"%.1f".format(photo.zoomLevel)}x zoom", style = MaterialTheme.typography.bodySmall, color = NeonGreen)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}
