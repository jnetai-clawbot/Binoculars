package com.jnetaol.binoculars.engine

import kotlin.math.abs
import kotlin.math.sqrt

object DistanceEstimator {

    data class DistanceResult(
        val estimatedDistanceMeters: Float,
        val confidencePercent: Float,
        val label: String
    )

    private const val REFERENCE_HEIGHT_METERS = 1.7f
    private const val FOCAL_LENGTH_MM = 4.3f
    private const val SENSOR_HEIGHT_MM = 4.0f
    private const val IMAGE_HEIGHT_PIXELS = 1920f
    private val KNOWN_OBJECT_SIZES = mapOf(
        "person" to 0.5f,
        "car" to 1.8f,
        "tree" to 0.3f,
        "door" to 1.0f
    )

    fun estimateDistance(
        zoomLevel: Float,
        objectFractionOfScreen: Float = 0.3f
    ): DistanceResult {
        val effectiveFocalLength = FOCAL_LENGTH_MM * zoomLevel
        val objectHeightSensor = objectFractionOfScreen * SENSOR_HEIGHT_MM

        var totalDistance = 0f
        var weightSum = 0f

        for ((name, realSize) in KNOWN_OBJECT_SIZES) {
            val distance = (realSize * effectiveFocalLength) / objectHeightSensor
            val weight = if (zoomLevel > 4f) 0.7f else 1.0f
            totalDistance += distance * weight
            weightSum += weight
        }

        val avgDistance = if (weightSum > 0) totalDistance / weightSum else 20f
        val confidence = ((1.0f - abs(zoomLevel - 3f) / 7f) * 80f).coerceIn(10f, 90f)

        val label = when {
            avgDistance < 5f -> "Very Close (~%.0fm)".format(avgDistance)
            avgDistance < 15f -> "Close (~%.0fm)".format(avgDistance)
            avgDistance < 50f -> "Mid Range (~%.0fm)".format(avgDistance)
            avgDistance < 200f -> "Far (~%.0fm)".format(avgDistance)
            else -> "Very Far (~%.0fm)".format(avgDistance)
        }

        return DistanceResult(
            estimatedDistanceMeters = avgDistance,
            confidencePercent = confidence,
            label = label
        )
    }
}
