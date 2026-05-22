package com.jnetaol.binoculars.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.jnetaol.binoculars.logger.DebugLogger
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var currentZoomRatio = 1f

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var onZoomChanged: ((Float) -> Unit)? = null

    fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCamera(previewView)
            } catch (e: Exception) {
                DebugLogger.e("CameraManager", "Failed to get camera provider", "CAM000", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(previewView: PreviewView) {
        val provider = cameraProvider ?: return
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(100)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            DebugLogger.i("CameraManager", "Camera bound successfully")
        } catch (e: Exception) {
            DebugLogger.e("CameraManager", "Failed to bind camera", "CAM001", e)
        }
    }

    fun setZoomRatio(ratio: Float) {
        val cameraControl = camera?.cameraControl ?: return
        val clampedRatio = ratio.coerceIn(1f, getMaxZoomRatio())
        try {
            cameraControl.setZoomRatio(clampedRatio)
        } catch (_: Exception) {}
        currentZoomRatio = clampedRatio
        onZoomChanged?.invoke(clampedRatio)
    }

    fun zoomIn(step: Float = 0.5f) { setZoomRatio(currentZoomRatio + step) }
    fun zoomOut(step: Float = 0.5f) { setZoomRatio(currentZoomRatio - step) }
    fun getCurrentZoomRatio(): Float = currentZoomRatio
    fun getMaxZoomRatio(): Float = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 8f
    fun getMinZoomRatio(): Float = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f

    fun setFlashlight(enabled: Boolean) {
        try {
            camera?.cameraControl?.enableTorch(enabled)
        } catch (_: Exception) {}
    }

    fun capturePhoto(onResult: (File?) -> Unit) {
        val ic = imageCapture ?: run {
            DebugLogger.e("CameraManager", "ImageCapture not initialized", "CAM002")
            scope.launch { onResult(null) }
            return
        }

        val outputDir = File(context.cacheDir, "photos")
        if (!outputDir.exists()) outputDir.mkdirs()
        val photoFile = File(outputDir, "BINOC_${System.currentTimeMillis()}.jpg")

        try {
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            ic.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        DebugLogger.i("CameraManager", "Photo saved: ${photoFile.name}")
                        scope.launch { onResult(photoFile) }
                    }
                    override fun onError(exception: ImageCaptureException) {
                        DebugLogger.e("CameraManager", "Capture error: ${exception.message}", "CAM003", exception)
                        scope.launch { onResult(null) }
                    }
                }
            )
        } catch (e: Exception) {
            DebugLogger.e("CameraManager", "Capture exception: ${e.message}", "CAM004", e)
            scope.launch { onResult(null) }
        }
    }

    fun release() {
        cameraExecutor.shutdown()
        scope.cancel()
    }
}
