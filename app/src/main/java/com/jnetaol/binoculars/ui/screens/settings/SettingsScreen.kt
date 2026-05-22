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
    val useScreenshotCapture by SettingsManager.useScreenshotCapture(context).collectAsState(initial = true)
    val saveLocation by SettingsManager.saveLocationMetadata(context).collectAsState(initial = false)
    val flashlightOn by SettingsManager.flashlightEnabled(context).collectAsState(initial = false)

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                }
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = "Viewfinder Overlays", accentColor = NeonGreen)

            NeonCard(borderColor = NeonGreen.copy(alpha = 0.2f)) {
                SettingToggle(Icons.Default.Straighten, "Distance Estimator", "Show estimated distance in viewfinder", showDistance,
                    { scope.launch { SettingsManager.setShowDistanceOverlay(context, it) } }, NeonGreen)
                Divider(color = TextTertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                SettingToggle(Icons.Default.GridOn, "Grid Overlay", "Show alignment grid in viewfinder", showGrid,
                    { scope.launch { SettingsManager.setShowGrid(context, it) } }, NeonTeal)
            }

            SectionHeader(title = "Capture", accentColor = AccentYellow)

            NeonCard(borderColor = AccentYellow.copy(alpha = 0.2f)) {
                SettingToggle(Icons.Default.Screenshot, "Screenshot Capture Mode", "Capture screen view instead of camera API (recommended, more reliable)", useScreenshotCapture,
                    { scope.launch { SettingsManager.setUseScreenshotCapture(context, it) } }, AccentYellow)
                Divider(color = TextTertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                SettingToggle(Icons.Default.LocationOn, "Save Location Metadata", "Embed GPS coordinates in captured photos", saveLocation,
                    { scope.launch { SettingsManager.setSaveLocationMetadata(context, it) } }, AccentBlue)
            }

            SectionHeader(title = "Display", accentColor = NeonTeal)

            NeonCard(borderColor = NeonTeal.copy(alpha = 0.2f)) {
                SettingToggle(Icons.Default.DarkMode, "Night Vision Mode", "Apply green night-vision filter to camera", nightVision,
                    { scope.launch { SettingsManager.setNightVisionMode(context, it) } }, NeonGreen)
                Divider(color = TextTertiary.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
                SettingToggle(Icons.Default.FlashlightOn, "Camera Flashlight (LED)", "Enable torch light for nighttime use", flashlightOn,
                    { scope.launch { SettingsManager.setFlashlightEnabled(context, it) } }, AccentYellow)
            }

            SectionHeader(title = "Information", accentColor = AccentBlue)

            NeonCard(borderColor = AccentBlue.copy(alpha = 0.2f)) {
                SettingRow(Icons.Default.Info, "About", "App info, version, and updates", onAbout, AccentBlue)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, accentColor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(
            checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha = 0.3f),
            uncheckedThumbColor = TextTertiary, uncheckedTrackColor = TextTertiary.copy(alpha = 0.2f)))
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit, accentColor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onClick) { Text("Open", color = accentColor) }
    }
}
