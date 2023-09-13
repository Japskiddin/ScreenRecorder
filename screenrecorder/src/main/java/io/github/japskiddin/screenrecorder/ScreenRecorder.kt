package io.github.japskiddin.screenrecorder

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
// TODO: Добавить обработку поворота экрана

class ScreenRecorder(
    activity: Activity,
    listener: ScreenRecorderListener
) {
    private val weakActivity: WeakReference<Activity>
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
        val activity = weakActivity.get()
        if (activity == null) {
            if (isServiceBound) {
                screenRecorderService?.stopRecord()
            } else {
                listener.onStopped()
            }
            return
        }

        if (isNotLolipop()) {
            showToast(activity, R.string.err_not_lolipop)
            return
        }

        if (screenRecorderService == null) {
            val intent = Intent(activity, ScreenRecorderService::class.java)
            ContextCompat.startForegroundService(activity, intent)
            activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            screenRecorderService?.startService()
            screenRecorderService?.startRecord()
        }
    }

    fun stop() {
        if (isServiceBound) {
            screenRecorderService?.stopRecord()
        } else {
            listener.onStopped()
        }
    }

    fun isRecording(): Boolean {
        return isRecording
    }

    private fun releaseService(connection: ServiceConnection) {
        isServiceBound = false
        screenRecorderService?.setListener(null)
        screenRecorderService = null
        weakActivity.get()?.unbindService(connection)
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
            screenRecorderService?.startRecord()
            isServiceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            releaseService(this)
        }
    }

    private val serviceListener: ServiceListener = object : ServiceListener {
        override fun onRecordStarted() {
            isRecording = true
            listener.onStarted()
        }

        override fun onRecordStopped(filepath: String?) {
            isRecording = false
            val activity = weakActivity.get() ?: return
            if (!activity.isFinishing) {
                listener.onStopped()
                listener.onCompleted(filepath)
                screenRecorderService?.stopService()
            }
        }

        override fun onStartActivity(intent: Intent?) {
            if (isRecordClicked || intent == null) return
            isRecordClicked = true
            try {
                recordVideoLauncher?.launch(intent)
            } catch (e: ActivityNotFoundException) {
                if (BuildConfig.DEBUG) Log.e(TAG, e.message.toString())
                showToast(activity, R.string.err_record_activity_not_found)
            }
        }

        override fun onServiceStopped() {
            releaseService(serviceConnection)
        }
    }

    companion object {
        private val TAG = ScreenRecorder::class.java.simpleName
    }
}