package com.jnetaol.binoculars.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jnetaol.binoculars.ui.components.NeonCard
import com.jnetaol.binoculars.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentVersion = remember {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "1.0.0"
        } catch (_: Exception) { "1.0.0" }
    }

    var updateStatus by remember { mutableStateOf("Checking...") }
    var showUpdate by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }
    var releaseUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://api.github.com/repos/jnetai-clawbot/Binoculars/releases/latest")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    conn.connectTimeout = 5000; conn.readTimeout = 5000
                    if (conn.responseCode == 200) {
                        val json = JSONObject(conn.inputStream.bufferedReader().readText())
                        Pair(json.getString("tag_name").removePrefix("v"), json.getString("html_url"))
                    } else null
                }
                if (result != null) {
                    latestVersion = result.first; releaseUrl = result.second
                    if (result.first != currentVersion) { updateStatus = "Update available: v${result.first}"; showUpdate = true }
                    else updateStatus = "You're up to date"
                } else updateStatus = "Could not check for updates"
            } catch (_: Exception) { updateStatus = "Could not check for updates" }
        }
    }

    Column(Modifier.fillMaxSize().background(DarkBackground)) {
        Surface(color = DarkSurface, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary) }
                Text("About", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            }
        }

        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(80.dp).background(NeonGreen.copy(alpha = 0.1f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.RemoveRedEye, null, tint = NeonGreen, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Binoculars", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = NeonGreen)
            Text("v$currentVersion", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("Made by jnetai.com", style = MaterialTheme.typography.bodyMedium, color = TextTertiary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))

            NeonCard(Modifier.fillMaxWidth(), borderColor = NeonGreen.copy(alpha = 0.2f)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(updateStatus, color = if (showUpdate) AccentYellow else NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    if (showUpdate) {
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jnetai-clawbot/Binoculars/releases/latest"))) }, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = TextOnAccent)) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Update", fontSize = 13.sp)
                            }
                            Button(onClick = {
                                val si = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Binoculars - Digital Zoom Camera"); putExtra(Intent.EXTRA_TEXT, "Check out Binoculars: https://github.com/jnetai-clawbot/Binoculars/releases/latest") }
                                context.startActivity(Intent.createChooser(si, "Share Binoculars"))
                            }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonTeal, contentColor = TextOnAccent)) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Digital zoom camera with distance estimation,\nnight vision mode, and photo gallery.", style = MaterialTheme.typography.bodySmall, color = TextTertiary, textAlign = TextAlign.Center)
        }
    }
}
