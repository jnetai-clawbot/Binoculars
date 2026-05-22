package com.jnetaol.binoculars.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jnetaol.binoculars.engine.SettingsManager
import com.jnetaol.binoculars.ui.components.NeonCard
import com.jnetaol.binoculars.ui.components.SectionHeader
import com.jnetaol.binoculars.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAbout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val showDistance by SettingsManager.showDistanceOverlay(context).collectAsState(initial = true)
    val nightVision by SettingsManager.nightVisionMode(context).collectAsState(initial = false)
    val showGrid by SettingsManager.showGrid(context).collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Surface(color = DarkSurface, shadowElevation = 4.dp) {
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
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = "Viewfinder Overlays", accentColor = NeonGreen)

            NeonCard(borderColor = NeonGreen.copy(alpha = 0.2f)) {
                SettingToggle(
                    icon = Icons.Default.Straighten,
                    title = "Distance Estimator",
                    subtitle = "Show estimated distance in viewfinder",
                    checked = showDistance,
                    onCheckedChange = {
                        scope.launch { SettingsManager.setShowDistanceOverlay(context, it) }
                    },
                    accentColor = NeonGreen
                )

                Divider(color = TextTertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                SettingToggle(
                    icon = Icons.Default.GridOn,
                    title = "Grid Overlay",
                    subtitle = "Show alignment grid in viewfinder",
                    checked = showGrid,
                    onCheckedChange = {
                        scope.launch { SettingsManager.setShowGrid(context, it) }
                    },
                    accentColor = NeonTeal
                )
            }

            SectionHeader(title = "Display", accentColor = NeonTeal)

            NeonCard(borderColor = NeonTeal.copy(alpha = 0.2f)) {
                SettingToggle(
                    icon = Icons.Default.DarkMode,
                    title = "Night Vision Mode",
                    subtitle = "Apply green night-vision filter to camera",
                    checked = nightVision,
                    onCheckedChange = {
                        scope.launch { SettingsManager.setNightVisionMode(context, it) }
                    },
                    accentColor = NeonGreen
                )
            }

            SectionHeader(title = "Information", accentColor = AccentBlue)

            NeonCard(borderColor = AccentBlue.copy(alpha = 0.2f)) {
                SettingRow(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "App info, version, and updates",
                    onClick = onAbout,
                    accentColor = AccentBlue
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.3f),
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = TextTertiary.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onClick) {
            Text("Open", color = accentColor)
        }
    }
}
