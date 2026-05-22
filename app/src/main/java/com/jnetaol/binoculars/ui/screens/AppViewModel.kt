package com.jnetaol.binoculars.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jnetaol.binoculars.BinocularsApp
import com.jnetaol.binoculars.data.db.AppDatabase
import com.jnetaol.binoculars.data.model.CapturedPhoto
import com.jnetaol.binoculars.engine.CameraManager
import com.jnetaol.binoculars.logger.DebugLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    fun setCapturing(capturing: Boolean) {
        _isCapturing.value = capturing
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
