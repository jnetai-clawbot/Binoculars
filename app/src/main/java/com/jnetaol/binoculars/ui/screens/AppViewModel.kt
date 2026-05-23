package com.jnetaol.binoculars.ui.screens

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jnetaol.binoculars.BinocularsApp
import com.jnetaol.binoculars.data.db.AppDatabase
import com.jnetaol.binoculars.data.model.CapturedPhoto
import com.jnetaol.binoculars.logger.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = (application as BinocularsApp).database
    private val photoDao = database.photoDao()

    val allPhotos = photoDao.getAllPhotos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val photoCount = photoDao.getPhotoCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun savePhoto(file: File, zoomLevel: Float) {
        viewModelScope.launch {
            try {
                val permanentDir = File(getApplication<Application>().filesDir, "gallery")
                if (!permanentDir.exists()) permanentDir.mkdirs()

                val permanentFile = File(permanentDir, file.name)
                FileInputStream(file).use { input ->
                    FileOutputStream(permanentFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val photo = CapturedPhoto(
                    filePath = permanentFile.absolutePath,
                    zoomLevel = zoomLevel
                )
                photoDao.insertPhoto(photo)
                _isCapturing.value = false
                DebugLogger.i("AppViewModel", "Photo saved to gallery: ${permanentFile.name}")
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Failed to save photo", "VM001", e)
                _isCapturing.value = false
                _toastMessage.emit("Failed to save photo")
            }
        }
    }

    fun deletePhoto(photo: CapturedPhoto) {
        viewModelScope.launch {
            try {
                val file = File(photo.filePath)
                if (file.exists()) file.delete()
                photoDao.deletePhoto(photo)
                DebugLogger.i("AppViewModel", "Photo deleted: ${photo.id}")
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Failed to delete photo", "VM002", e)
                _toastMessage.emit("Failed to delete photo")
            }
        }
    }

    fun deleteAllPhotos() {
        viewModelScope.launch {
            try {
                val photos = photoDao.getAllPhotos().first()
                photos.forEach { photo ->
                    val file = File(photo.filePath)
                    if (file.exists()) file.delete()
                }
                photoDao.deleteAll()
                DebugLogger.i("AppViewModel", "All photos deleted")
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "Failed to delete all photos", "VM003", e)
                _toastMessage.emit("Failed to clear gallery")
            }
        }
    }

    fun setCapturing(capturing: Boolean) { _isCapturing.value = capturing }

    fun moveToDcim(photos: List<CapturedPhoto>) {
        viewModelScope.launch {
            var moved = 0
            try {
                withContext(Dispatchers.IO) {
                    photos.forEach { photo ->
                        val src = File(photo.filePath)
                        if (!src.exists()) return@forEach
                        val app = getApplication<Application>()
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val values = ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, "Binoculars_${System.currentTimeMillis()}_${photo.id}.jpg")
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Binoculars")
                                }
                                val uri = app.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                uri?.let { app.contentResolver.openOutputStream(it)?.use { out -> FileInputStream(src).use { it.copyTo(out) } } }
                                if (uri != null) moved++
                            } else {
                                val dcim = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Binoculars")
                                if (!dcim.exists()) dcim.mkdirs()
                                FileInputStream(src).use { input -> FileOutputStream(File(dcim, src.name)).use { output -> input.copyTo(output) } }
                                moved++
                            }
                        } catch (_: Exception) {}
                    }
                }
                _toastMessage.emit("Moved $moved photo(s) to DCIM/Binoculars")
            } catch (e: Exception) {
                DebugLogger.e("AppViewModel", "DCIM move failed", "VM004", e)
                _toastMessage.emit("Failed to move photos to DCIM")
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
                return AppViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
