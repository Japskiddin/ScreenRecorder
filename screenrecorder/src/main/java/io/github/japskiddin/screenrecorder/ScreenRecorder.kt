package io.github.japskiddin.screenrecorder

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import io.github.japskiddin.screenrecorder.contract.RecordVideo
import io.github.japskiddin.screenrecorder.interfaces.ScreenRecorderListener
import io.github.japskiddin.screenrecorder.interfaces.ServiceListener
import io.github.japskiddin.screenrecorder.model.RecordVideoResult
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService.Companion.EXTRA_RECORDER_CODE
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService.Companion.EXTRA_RECORDER_DATA
import io.github.japskiddin.screenrecorder.utils.isServiceDead
import io.github.japskiddin.screenrecorder.utils.showToast
import java.lang.ref.WeakReference

// TODO: Сделать по паттерну Билдер?
// TODO: Добавить переводы строк
// TODO: добавить паузу?

class ScreenRecorder(
    activity: Activity,
    listener: ScreenRecorderListener
) {
    private val weakActivity: WeakReference<AppCompatActivity>
    private val listener: ScreenRecorderListener

    private var isRecording = false
    private var isServiceBound = false
    private var isRecordClicked = false
    private var screenRecorderService: ScreenRecorderService? = null
    private var recordVideoLauncher: ActivityResultLauncher<Intent>? = null

    init {
        if (activity is AppCompatActivity) {
            weakActivity = WeakReference(activity)
            recordVideoLauncher = activity.registerForActivityResult(RecordVideo()) { result ->
                parseRecordIntent(result)
            }
        } else {
            throw ClassCastException("AppCompatActivity requires.")
        }
        this.listener = listener
    }

    fun start() {
        if (isRecording) {
            stop()
        }

        if (screenRecorderService == null) {
            bindService(Context.BIND_AUTO_CREATE)
        } else {
            screenRecorderService?.startService()
            if (!isRecording) {
                screenRecorderService?.startRecord()
            }
        }
    }

    fun stop() {
        if (isServiceBound) {
            screenRecorderService?.stopRecord()
        } else {
            listener.onStopped()
        }
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putBoolean(EXTRA_IS_RECORD_RUNNING, isRecording)
    }

    fun restoreInstanceState(savedInstanceState: Bundle?) {
        isRecording = savedInstanceState?.getBoolean(EXTRA_IS_RECORD_RUNNING, false) ?: false
        if (!isRecording || isServiceDead(weakActivity.get())) return
        listener.onRestored()
        bindService(0)
    }

    fun isRecording(): Boolean {
        return isRecording
    }

    private fun bindService(flag: Int) {
        weakActivity.get()?.let {
            val intent = Intent(it, ScreenRecorderService::class.java)
            it.bindService(intent, serviceConnection, flag)
        }
    }

    private fun releaseService() {
        if (isServiceBound) {
            screenRecorderService?.setListener(null)
            screenRecorderService = null
            weakActivity.get()?.unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun parseRecordIntent(result: RecordVideoResult) {
        val intent = Intent(weakActivity.get(), ScreenRecorderService::class.java).apply {
            putExtra(EXTRA_RECORDER_CODE, result.code)
            putExtra(EXTRA_RECORDER_DATA, result.intent)
        }
        screenRecorderService?.parseIntent(intent)
        isRecordClicked = false
    }

    /**
     * Callbacks for service binding, passed to bindService()
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ScreenRecorderService.ScreenRecorderBinder
            screenRecorderService = binder.getService()
            screenRecorderService?.setListener(serviceListener)
            screenRecorderService?.startService()
            if (!isRecording) {
                screenRecorderService?.startRecord()
            }
            isServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            releaseService()
        }
    }

    private val serviceListener = object : ServiceListener {
        override fun onRecordStarted() {
            isRecording = true
            listener.onStarted()
        }

        override fun onRecordStopped(filepath: String?) {
            isRecording = false
            listener.onStopped()
            listener.onCompleted(filepath)
            screenRecorderService?.stopService()
        }

        override fun onStartActivity(intent: Intent?) {
            if (isRecordClicked || intent == null) return
            isRecordClicked = true
            try {
                recordVideoLauncher?.launch(intent)
            } catch (e: ActivityNotFoundException) {
                if (BuildConfig.DEBUG) Log.e(TAG, e.message.toString())
                weakActivity.get()?.let {
                    showToast(it, R.string.err_record_activity_not_found)
                }
            }
        }

        override fun onServiceStopped() {
            releaseService()
        }
    }

    companion object {
        private val TAG = ScreenRecorder::class.java.simpleName
        private const val EXTRA_IS_RECORD_RUNNING = "EXTRA_IS_RECORD_RUNNING"
    }
}