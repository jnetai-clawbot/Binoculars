package com.jnetaol.binoculars.ui.screens.gallery

import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun GalleryScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val photos by viewModel.allPhotos.collectAsState()
    var selectedPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        if (selectedPhoto != null) {
            PhotoDetailScreen(
                photo = selectedPhoto!!,
                onClose = { selectedPhoto = null },
                onDelete = {
                    showDeleteConfirm = true
                },
                isDeleting = showDeleteConfirm
            )

            if (showDeleteConfirm) {
                ConfirmDialog(
                    title = "Delete Photo",
                    message = "This action cannot be undone.",
                    onConfirm = {
                        viewModel.deletePhoto(selectedPhoto!!)
                        selectedPhoto = null
                        showDeleteConfirm = false
                    },
                    onDismiss = { showDeleteConfirm = false }
                )
            }

            return
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = DarkSurface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Text(
                        text = "Gallery",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    if (photos.isNotEmpty()) {
                        IconButton(onClick = { showClearAllConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Delete all",
                                tint = AccentRed.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (photos.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Gallery is empty",
                    subtitle = "Photos you capture will appear here"
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        GalleryThumbnail(
                            photo = photo,
                            onClick = { selectedPhoto = photo }
                        )
                    }
                }
            }
        }
    }

    if (showClearAllConfirm) {
        ConfirmDialog(
            title = "Delete All Photos",
            message = "This will permanently delete all ${photos.size} photos. This action cannot be undone.",
            confirmText = "Delete All",
            onConfirm = {
                viewModel.deleteAllPhotos()
                showClearAllConfirm = false
            },
            onDismiss = { showClearAllConfirm = false }
        )
    }
}

@Composable
private fun GalleryThumbnail(
    photo: CapturedPhoto,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val file = File(photo.filePath)
        if (file.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "Photo ${photo.id}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Missing",
                tint = TextTertiary,
                modifier = Modifier.size(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "%.1fx".format(photo.zoomLevel),
                color = NeonGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PhotoDetailScreen(
    photo: CapturedPhoto,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    isDeleting: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val file = File(photo.filePath)
        if (file.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "Photo ${photo.id}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Image not found", color = TextSecondary)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = AccentRed
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = Color.Black.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(icon = Icons.Default.ZoomIn, label = "%.1fx zoom".format(photo.zoomLevel))
                    InfoChip(
                        icon = Icons.Default.Image,
                        label = formatFileSize(file.length())
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dateFormat.format(Date(photo.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Text(
                    text = photo.filePath.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonGreen,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
