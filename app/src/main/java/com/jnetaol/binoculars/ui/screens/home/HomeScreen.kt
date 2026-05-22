package com.jnetaol.binoculars.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.binoculars.ui.components.*
import com.jnetaol.binoculars.ui.screens.AppViewModel
import com.jnetaol.binoculars.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit
) {
    val photoCount by viewModel.photoCount.collectAsState()
    val recentPhotos by viewModel.allPhotos.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Binoculars",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Text(
                        text = "Digital Zoom Camera",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            NeonCard(borderColor = NeonGreen.copy(alpha = 0.3f)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "See Farther, Capture Closer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use digital zoom to magnify distant objects and capture high-resolution photos. All photos saved locally to your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (hasPermission) {
                        GlowButton(
                            text = "Open Viewfinder",
                            icon = Icons.Default.Camera,
                            onClick = onOpenCamera,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = NeonGreen,
                            contentColor = TextOnAccent
                        )
                    } else {
                        GlowButton(
                            text = "Grant Camera Permission",
                            icon = Icons.Default.Lock,
                            onClick = onRequestPermission,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = AccentYellow,
                            contentColor = TextOnAccent
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Photos",
                    value = "$photoCount",
                    icon = Icons.Default.PhotoLibrary,
                    color = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Gallery",
                    value = "View",
                    icon = Icons.Default.Collections,
                    color = NeonTeal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionHeader(
                title = "Quick Actions",
                accentColor = NeonGreen
            )
        }

        item {
            NeonCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (hasPermission) onOpenCamera() else onRequestPermission()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(NeonGreen.copy(alpha = 0.5f))
                        )
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Viewfinder", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenGallery,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonTeal),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(NeonTeal.copy(alpha = 0.5f))
                        )
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Recent Captures",
                accentColor = NeonTeal
            )
        }

        if (recentPhotos.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.CameraAlt,
                    title = "No photos yet",
                    subtitle = "Open the viewfinder to start capturing zoomed photos"
                )
            }
        } else {
            items(recentPhotos.size.coerceAtMost(6)) { index ->
                val photo = recentPhotos[index]
                NeonCard(
                    borderColor = ClearColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = NeonGreenDim,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = photo.filePath.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "${"%.1f".format(photo.zoomLevel)}x zoom",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonGreen
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
