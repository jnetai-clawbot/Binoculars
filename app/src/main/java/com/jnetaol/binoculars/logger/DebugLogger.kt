package com.jnetaol.binoculars.logger

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private const val TAG = "Binoculars"
    private const val ERROR_CODE_PREFIX = "BN-"
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(logDir: File) {
        logFile = File(logDir, "binoculars_debug.log")
        i("DebugLogger", "Logger initialized at ${logFile?.absolutePath}")
    }

    fun d(tag: String, message: String) {
        val msg = "[DEBUG] $message"
        Log.d(TAG, "[$tag] $msg")
        appendToFile(tag, "DEBUG", message)
    }

    fun i(tag: String, message: String) {
        val msg = "[INFO] $message"
        Log.i(TAG, "[$tag] $msg")
        appendToFile(tag, "INFO", message)
    }

    fun w(tag: String, message: String, errorCode: String? = null) {
        val codeStr = errorCode?.let { " [$ERROR_CODE_PREFIX$it]" } ?: ""
        val msg = "[WARN]$codeStr $message"
        Log.w(TAG, "[$tag] $msg")
        appendToFile(tag, "WARN", "$codeStr $message")
    }

    fun e(tag: String, message: String, errorCode: String, throwable: Throwable? = null) {
        val codeStr = "$ERROR_CODE_PREFIX$errorCode"
        val msg = "[ERROR][$codeStr] $message"
        Log.e(TAG, "[$tag] $msg", throwable)
        appendToFile(tag, "ERROR", "[$codeStr] $message")
        throwable?.let {
            appendToFile(tag, "ERROR", "  Caused by: ${it.message}")
            it.stackTraceToString().lines().take(5).forEach { line ->
                appendToFile(tag, "ERROR", "    $line")
            }
        }
    }

    private fun appendToFile(tag: String, level: String, message: String) {
        try {
            logFile?.let { file ->
                if (!file.exists()) file.createNewFile()
                if (file.length() > 5 * 1024 * 1024) {
                    file.writeText("")
                }
                FileWriter(file, true).use { writer ->
                    writer.append("${dateFormat.format(Date())} [$level] [$tag] $message\n")
                }
            }
        } catch (_: Exception) {
        }
    }
}
