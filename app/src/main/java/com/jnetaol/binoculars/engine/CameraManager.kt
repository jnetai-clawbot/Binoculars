package com.jnetaol.binoculars.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.jnetaol.binoculars.logger.DebugLogger
import java.io.File

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var currentZoomRatio = 1f

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    var onZoomChanged: ((Float) -> Unit)? = null

    fun startCamera(previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            bindCamera(previewView)
        }, mainExecutor)
    }

    private fun bindCamera(previewView: PreviewView) {
        val provider = cameraProvider ?: return
        val selector = CameraSelector.DEFAULT_BACK_CAMERA

        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setJpegQuality(100)
            .build()

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            DebugLogger.i("CameraManager", "Camera bound")
        } catch (e: Exception) {
            DebugLogger.e("CameraManager", "Bind failed", "CAM001", e)
        }
    }

    fun setZoomRatio(ratio: Float) {
        val cc = camera?.cameraControl ?: return
        val clamped = ratio.coerceIn(1f, getMaxZoomRatio())
        try { cc.setZoomRatio(clamped) } catch (_: Exception) {}
        currentZoomRatio = clamped
        onZoomChanged?.invoke(clamped)
    }

    fun zoomIn(step: Float = 0.5f) { setZoomRatio(currentZoomRatio + step) }
    fun zoomOut(step: Float = 0.5f) { setZoomRatio(currentZoomRatio - step) }
    fun getMaxZoomRatio(): Float = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 8f

    fun setFlashlight(enabled: Boolean) {
        try { camera?.cameraControl?.enableTorch(enabled) } catch (_: Exception) {}
    }

    fun capturePhoto(onResult: (File?) -> Unit) {
        val ic = imageCapture
        if (ic == null) {
            DebugLogger.e("CameraManager", "No ImageCapture", "CAM002")
            mainHandler.post { onResult(null) }
            return
        }

        val dir = File(context.filesDir, "photos")
        if (!dir.exists()) dir.mkdirs()
        val photoFile = File(dir, "BINOC_${System.currentTimeMillis()}.jpg")

        ic.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    DebugLogger.i("CameraManager", "Photo saved: ${photoFile.name} ${photoFile.length()}b")
                    mainHandler.post { onResult(photoFile) }
                }
                override fun onError(exc: ImageCaptureException) {
                    DebugLogger.e("CameraManager", "Capture failed[${exc.imageCaptureError}]: ${exc.message}", "CAM003", exc)
                    mainHandler.post { onResult(null) }
                }
            }
        )
    }

    fun release() {}
}
