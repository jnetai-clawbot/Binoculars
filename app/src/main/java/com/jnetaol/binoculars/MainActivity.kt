package com.jnetaol.binoculars

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jnetaol.binoculars.engine.CameraManager
import com.jnetaol.binoculars.ui.screens.AppViewModel
import com.jnetaol.binoculars.ui.screens.about.AboutScreen
import com.jnetaol.binoculars.ui.screens.camera.CameraScreen
import com.jnetaol.binoculars.ui.screens.gallery.GalleryScreen
import com.jnetaol.binoculars.ui.screens.home.HomeScreen
import com.jnetaol.binoculars.ui.screens.settings.SettingsScreen
import com.jnetaol.binoculars.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Binoculars", Icons.Default.RemoveRedEye)
    object Camera : Screen("camera", "Viewfinder", Icons.Default.Camera)
    object Gallery : Screen("gallery", "Gallery", Icons.Default.PhotoLibrary)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object About : Screen("about", "About", Icons.Default.Info)
}

class MainActivity : ComponentActivity() {

    private var cameraPermissionGranted = false
    private var currentScreen: Screen = Screen.Home
    private var cameraManager: CameraManager? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (currentScreen == Screen.Camera) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        cameraManager?.zoomIn()
                        return true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        cameraManager?.zoomOut()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            BinocularsTheme {
                val app = applicationContext as BinocularsApp
                val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(app))

                val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle(initialValue = null)
                var screenState by remember { mutableStateOf<Screen>(Screen.Home) }

                val bottomNavItems = listOf(Screen.Home, Screen.Gallery, Screen.Settings)

                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(screenState) {
                    currentScreen = screenState
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = {
                        if (screenState != Screen.Camera && screenState != Screen.About) {
                            NavigationBar(
                                containerColor = DarkSurface,
                                contentColor = TextPrimary,
                                tonalElevation = 8.dp
                            ) {
                                bottomNavItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.title
                                            )
                                        },
                                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                                        selected = screenState == screen,
                                        onClick = { screenState = screen },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = NeonGreen,
                                            selectedTextColor = NeonGreen,
                                            unselectedIconColor = TextTertiary,
                                            unselectedTextColor = TextTertiary,
                                            indicatorColor = NeonGreen.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        color = DarkBackground
                    ) {
                        when (screenState) {
                            Screen.Home -> HomeScreen(
                                viewModel = viewModel,
                                hasPermission = cameraPermissionGranted,
                                onRequestPermission = {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                onOpenCamera = { screenState = Screen.Camera },
                                onOpenGallery = { screenState = Screen.Gallery },
                                onOpenSettings = { screenState = Screen.Settings }
                            )

                            Screen.Camera -> CameraScreen(
                                onClose = { screenState = Screen.Home },
                                onPhotoCaptured = { file, zoom ->
                                    viewModel.savePhoto(file, zoom)
                                    screenState = Screen.Home
                                },
                                onCameraReady = { cm ->
                                    cameraManager = cm
                                }
                            )

                            Screen.Gallery -> GalleryScreen(
                                viewModel = viewModel,
                                onBack = { screenState = Screen.Home }
                            )

                            Screen.Settings -> SettingsScreen(
                                onBack = { screenState = Screen.Home },
                                onAbout = { screenState = Screen.About }
                            )

                            Screen.About -> AboutScreen(
                                onBack = { screenState = Screen.Settings }
                            )
                        }
                    }
                }
            }
        }
    }
}
