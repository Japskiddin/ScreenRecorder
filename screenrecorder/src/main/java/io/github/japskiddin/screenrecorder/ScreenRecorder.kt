package io.github.japskiddin.screenrecorder

import android.app.Activity
import android.app.ActivityManager
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
import androidx.core.content.ContextCompat
import io.github.japskiddin.screenrecorder.contract.RecordVideo
import io.github.japskiddin.screenrecorder.interfaces.ScreenRecorderListener
import io.github.japskiddin.screenrecorder.interfaces.ServiceListener
import io.github.japskiddin.screenrecorder.model.RecordVideoResult
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService.Companion.EXTRA_RECORDER_CODE
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService.Companion.EXTRA_RECORDER_DATA
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService.LocalBinder
import io.github.japskiddin.screenrecorder.utils.isNotLolipop
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
        if (isNotLolipop()) {
            weakActivity.get()?.let {
                showToast(it, R.string.err_not_lolipop)
            }
            return
        }

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

    fun saveState(outState: Bundle) {
        outState.putBoolean(EXTRA_IS_RECORD_RUNNING, isRecording)
    }

    fun restoreState(savedInstanceState: Bundle?) {
        isRecording = savedInstanceState?.getBoolean(EXTRA_IS_RECORD_RUNNING, false) ?: false
        if (!isRecording) return
        var isServiceAlive = false
        val manager =
            weakActivity.get()?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (ScreenRecorderService::class.java.name.equals(service.service.className)) {
                isServiceAlive = true
                break
            }
        }
        if (!isServiceAlive) return
        listener.onRestored()
        bindService(0)
    }

    fun isRecording(): Boolean {
        return isRecording
    }

    private fun bindService(flag: Int) {
        weakActivity.get()?.let {
            val intent = Intent(it, ScreenRecorderService::class.java)
            ContextCompat.startForegroundService(it, intent)
            it.bindService(intent, serviceConnection, flag)
        }
    }

    private fun releaseService() {
        isServiceBound = false
        screenRecorderService?.setListener(null)
        screenRecorderService = null
        weakActivity.get()?.unbindService(serviceConnection)
    }

    private fun parseRecordIntent(result: RecordVideoResult) {
        val intent = Intent(weakActivity.get(), ScreenRecorderService::class.java)
        intent.putExtra(EXTRA_RECORDER_CODE, result.code)
        intent.putExtra(EXTRA_RECORDER_DATA, result.intent)
        screenRecorderService?.parseIntent(intent)
        isRecordClicked = false
    }

    /**
     * Callbacks for service binding, passed to bindService()
     */
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocalBinder
            screenRecorderService = binder.service
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

    private val serviceListener: ServiceListener = object : ServiceListener {
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