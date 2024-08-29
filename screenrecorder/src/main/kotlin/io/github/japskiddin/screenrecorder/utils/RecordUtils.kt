package io.github.japskiddin.screenrecorder.utils

import android.content.Context
import android.content.res.Configuration
import android.media.CamcorderProfile
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import io.github.japskiddin.screenrecorder.model.CameraInfo
import io.github.japskiddin.screenrecorder.model.DisplayInfo
import io.github.japskiddin.screenrecorder.model.RecordingInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val curSysDate: String
  get() = SimpleDateFormat("MM-dd_HH-mm", Locale.getDefault()).format(Date())

internal fun getRecordingInfo(context: Context): RecordingInfo {
  val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  val display: DisplayInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    windowManager.currentWindowMetrics.run {
      DisplayInfo(
        width = bounds.width(),
        height = bounds.height(),
        density = context.resources.configuration.densityDpi
      )
    }
  } else {
    DisplayMetrics().run {
      @Suppress("DEPRECATION")
      windowManager.defaultDisplay.getRealMetrics(this)
      DisplayInfo(
        width = widthPixels,
        height = heightPixels,
        density = densityDpi
      )
    }
  }
  val configuration = context.resources.configuration
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  val camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
  val camera: CameraInfo = if (camcorderProfile != null) {
    CameraInfo(
      width = camcorderProfile.videoFrameWidth,
      height = camcorderProfile.videoFrameHeight,
      frameRate = camcorderProfile.videoFrameRate
    )
  } else {
    CameraInfo(width = -1, height = -1, frameRate = 30)
  }
  return calculateRecordingInfo(
    display,
    isLandscape,
    camera
  )
}

private fun calculateRecordingInfo(
  display: DisplayInfo,
  isLandscapeDevice: Boolean,
  camera: CameraInfo,
  sizePercentage: Int = 100,
): RecordingInfo {
  // Scale the display size before any maximum size calculations.
  val width = display.width * sizePercentage / 100
  val height = display.height * sizePercentage / 100
  if (camera.width == -1 && camera.height == -1) {
    // No cameras. Fall back to the display size.
    return RecordingInfo(width, height, camera.frameRate, display.density)
  }
  var frameWidth: Int
  var frameHeight: Int
  if (isLandscapeDevice) {
    frameWidth = camera.width
    frameHeight = camera.height
  } else {
    frameWidth = camera.height
    frameHeight = camera.width
  }
  if (frameWidth >= width && frameHeight >= height) {
    // Frame can hold the entire display. Use exact values.
    return RecordingInfo(width, height, camera.frameRate, display.density)
  }

  // Calculate new width or height to preserve aspect ratio.
  if (isLandscapeDevice) {
    frameWidth = width * frameHeight / height
  } else {
    frameHeight = height * frameWidth / width
  }
  return RecordingInfo(frameWidth, frameHeight, camera.frameRate, display.density)
}
