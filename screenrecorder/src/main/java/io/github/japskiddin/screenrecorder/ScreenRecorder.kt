package io.github.japskiddin.screenrecorder

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.util.Log
import java.util.Locale

public class ScreenRecorder(context: Context) {
  public interface ScreenRecorderListener {
    public fun recorderOnStart()

    public fun recorderOnComplete(filepath: String?)

    public fun recorderOnError(errorMessage: Int)
  }

  private val receiver: ResultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
      super.onReceiveResult(resultCode, resultData)
      if (resultCode == Activity.RESULT_OK) {
        val errorListener =
          resultData.getString(ScreenRecorderService.EXTRA_ERROR_REASON_KEY)
        val onComplete =
          resultData.getString(ScreenRecorderService.EXTRA_ON_COMPLETE_KEY)
        val onStartCode = resultData.getInt(ScreenRecorderService.EXTRA_ON_START_KEY)
        val errorCode = resultData.getInt(ScreenRecorderService.EXTRA_ERROR_KEY)
        // There was an error
        if (errorListener != null) {
          if (BuildConfig.DEBUG) {
            val code =
              if (errorCode > 0) errorCode else ScreenRecorderService.GENERAL_ERROR
            Log.e(
              TAG,
              String.format(Locale.getDefault(), "%d: %s", code, errorListener)
            )
          }
          recorderListener?.recorderOnError(R.string.err_screen_record)
          try {
            stopScreenRecording()
          } catch (ignored: Exception) {
          }
        } else if (onComplete != null) {
          // OnComplete was called
          val filepath =
            resultData.getString(ScreenRecorderService.EXTRA_FILE_PATH_KEY, "")
          if (filepath.isNotEmpty()) {
            recorderListener?.recorderOnComplete(filepath)
          }
        } else if (onStartCode != 0) {
          // OnStart was called
          recorderListener?.recorderOnStart()
        }
      }
    }
  }
  private val context: Context = context.applicationContext
  private var recorderListener: ScreenRecorderListener? = null

  init {
    if (isBusyRecording) {
      val service = Intent(context, ScreenRecorderService::class.java).apply {
        action = ScreenRecorderService.ACTION_ATTACH_LISTENER
        putExtra(ScreenRecorderService.EXTRA_BUNDLED_LISTENER, receiver)
      }
      context.startService(service)
    }
  }

  public fun setListener(listener: ScreenRecorderListener?) {
    this.recorderListener = listener
  }

  public fun startScreenRecording(resultCode: Int, data: Intent) {
    startService(resultCode, data)
  }

  public fun stopScreenRecording() {
    val service = Intent(context, ScreenRecorderService::class.java)
    context.stopService(service)
  }

  public val isBusyRecording: Boolean
    get() {
      val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      @Suppress("DEPRECATION")
      for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (ScreenRecorderService::class.java.name == service.service.className) {
          return true
        }
      }
      return false
    }

  private fun startService(code: Int, data: Intent) {
    try {
      val service = Intent(context, ScreenRecorderService::class.java).apply {
        putExtra(ScreenRecorderService.EXTRA_RESULT_CODE_KEY, code)
        putExtra(ScreenRecorderService.EXTRA_RESULT_DATA_KEY, data)
        putExtra(ScreenRecorderService.EXTRA_BUNDLED_LISTENER, receiver)
      }
      context.startService(service)
    } catch (e: Exception) {
      recorderListener?.recorderOnError(R.string.err_screen_record)
    }
  }

  private companion object {
    private val TAG: String = ScreenRecorder::class.java.simpleName
  }
}
