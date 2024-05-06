package io.github.japskiddin.screenrecorder

import android.content.Context
import android.content.res.Configuration
import android.media.CamcorderProfile
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val curSysDate: String
    get() = SimpleDateFormat("MM-dd_HH-mm", Locale.getDefault()).format(Date())

internal fun getRecordingInfo(context: Context): RecordingInfo {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val displayWidth: Int
    val displayHeight: Int
    val displayDensity: Int
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = wm.currentWindowMetrics
        displayWidth = windowMetrics.bounds.width()
        displayHeight = windowMetrics.bounds.height()
        displayDensity = context.resources.configuration.densityDpi
    } else {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(displayMetrics)
        displayWidth = displayMetrics.widthPixels
        displayHeight = displayMetrics.heightPixels
        displayDensity = displayMetrics.densityDpi
    }
    val configuration = context.resources.configuration
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
    var cameraWidth = -1
    var cameraHeight = -1
    var cameraFrameRate = 30
    if (camcorderProfile != null) {
        cameraWidth = camcorderProfile.videoFrameWidth
        cameraHeight = camcorderProfile.videoFrameHeight
        cameraFrameRate = camcorderProfile.videoFrameRate
    }
    return calculateRecordingInfo(
        displayWidth,
        displayHeight,
        displayDensity,
        isLandscape,
        cameraWidth,
        cameraHeight,
        cameraFrameRate,
        100
    )
}

internal fun calculateRecordingInfo(
    displayWidth: Int, displayHeight: Int,
    displayDensity: Int,
    isLandscapeDevice: Boolean,
    cameraWidth: Int,
    cameraHeight: Int,
    cameraFrameRate: Int,
    sizePercentage: Int,
): RecordingInfo {
    // Scale the display size before any maximum size calculations.
    val width = displayWidth * sizePercentage / 100
    val height = displayHeight * sizePercentage / 100
    if (cameraWidth == -1 && cameraHeight == -1) {
        // No cameras. Fall back to the display size.
        return RecordingInfo(width, height, cameraFrameRate, displayDensity)
    }
    var frameWidth: Int
    var frameHeight: Int
    if (isLandscapeDevice) {
        frameWidth = cameraWidth
        frameHeight = cameraHeight
    } else {
        frameWidth = cameraHeight
        frameHeight = cameraWidth
    }
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