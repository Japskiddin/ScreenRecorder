package io.github.japskiddin.screenrecorder.utils

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.media.CamcorderProfile
import android.util.DisplayMetrics
import android.view.WindowManager
import io.github.japskiddin.screenrecorder.model.RecordingInfo

fun getRecordingInfo(context: Context): RecordingInfo {
    val displayMetrics = DisplayMetrics()
    val wm = context.getSystemService(WINDOW_SERVICE) as WindowManager
    wm.defaultDisplay.getRealMetrics(displayMetrics)
    val displayWidth = displayMetrics.widthPixels
    val displayHeight = displayMetrics.heightPixels
    val displayDensity = displayMetrics.densityDpi
    val configuration: Configuration = context.resources.configuration
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE
    val camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
    val cameraWidth = camcorderProfile?.videoFrameWidth ?: -1
    val cameraHeight = camcorderProfile?.videoFrameHeight ?: -1
    val cameraFrameRate = camcorderProfile?.videoFrameRate ?: 30
    return calculateRecordingInfo(
        displayWidth, displayHeight, displayDensity, isLandscape,
        cameraWidth, cameraHeight, cameraFrameRate, 100
    )
}

private fun calculateRecordingInfo(
    displayWidth: Int,
    displayHeight: Int,
    displayDensity: Int,
    isLandscapeDevice: Boolean,
    cameraWidth: Int,
    cameraHeight: Int,
    cameraFrameRate: Int,
    sizePercentage: Int
): RecordingInfo {
    // Scale the display size before any maximum size calculations.
    val width = displayWidth * sizePercentage / 100
    val height = displayHeight * sizePercentage / 100
    if (cameraWidth == -1 && cameraHeight == -1) {
        // No cameras. Fall back to the display size.
        return RecordingInfo(width, height, cameraFrameRate, displayDensity)
    }
    var frameWidth = if (isLandscapeDevice) cameraWidth else cameraHeight
    var frameHeight = if (isLandscapeDevice) cameraHeight else cameraWidth
    if (frameWidth >= width && frameHeight >= height) {
        // Frame can hold the entire display. Use exact values.
        return RecordingInfo(width, height, cameraFrameRate, displayDensity)
    }

    // Calculate new width or height to preserve aspect ratio.
    if (isLandscapeDevice) {
        frameWidth = width * frameHeight / height
    } else {
        frameHeight = height * frameWidth / width
    }
    return RecordingInfo(frameWidth, frameHeight, cameraFrameRate, displayDensity)
}