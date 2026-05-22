package com.jnetaol.binoculars

import android.app.Application
import com.jnetaol.binoculars.data.db.AppDatabase
import com.jnetaol.binoculars.logger.DebugLogger

class BinocularsApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        DebugLogger.init(filesDir)
        DebugLogger.i("BinocularsApp", "Binoculars v1.0.0 starting")
    }
}
