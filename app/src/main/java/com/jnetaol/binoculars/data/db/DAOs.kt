package com.jnetaol.binoculars.data.db

import androidx.room.*
import com.jnetaol.binoculars.data.model.CapturedPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM captured_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<CapturedPhoto>>

    @Query("SELECT * FROM captured_photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): CapturedPhoto?

    @Query("SELECT COUNT(*) FROM captured_photos")
    fun getPhotoCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: CapturedPhoto): Long

    @Delete
    suspend fun deletePhoto(photo: CapturedPhoto)

    @Query("DELETE FROM captured_photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM captured_photos")
    suspend fun deleteAll()
}
