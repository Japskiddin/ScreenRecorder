package io.github.japskiddin.screenrecorder.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.CamcorderProfile
import android.media.projection.MediaProjection
import android.os.Build
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import io.github.japskiddin.screenrecorder.model.Metrics
import io.github.japskiddin.screenrecorder.model.RecordingInfo
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getMetrics(context: Context): Metrics {
    val wm = context.getSystemService(WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = wm.currentWindowMetrics
        val bounds = windowMetrics.bounds
        Metrics(bounds.width(), bounds.height(), context.resources.configuration.densityDpi)
    } else {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(displayMetrics)
        Metrics(displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi)
    }
}

fun isServiceDead(context: Context?): Boolean {
    val manager = context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (ScreenRecorderService::class.java.name.equals(service.service.className)) {
            return false
        }
    }
    return true
}

fun showToast(context: Context, @StringRes message: Int) {
    Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
}

fun getSysDate(): String {
    return SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
}

fun isLandscape(metrics: Metrics): Boolean {
    return metrics.width > metrics.height
}

fun getVirtualDisplay(
    context: Context,
    className: String,
    mediaProjection: MediaProjection?,
    surface: Surface
): VirtualDisplay? {
    val metrics = getMetrics(context)
    return mediaProjection?.createVirtualDisplay(
        className,
        metrics.width,
        metrics.height,
        metrics.density,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        surface,
        null,
        null
    )
}

fun getRecordingInfo(context: Context): RecordingInfo {
    val camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
    val cameraWidth = camcorderProfile?.videoFrameWidth ?: -1
    val cameraHeight = camcorderProfile?.videoFrameHeight ?: -1
    val cameraFrameRate = camcorderProfile?.videoFrameRate ?: 30
    val metrics = getMetrics(context)
    return calculateRecordingInfo(
        metrics.width, metrics.height, metrics.density, isLandscape(metrics),
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