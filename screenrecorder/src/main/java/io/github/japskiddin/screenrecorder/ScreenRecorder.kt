package io.github.japskiddin.screenrecorder

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import io.github.japskiddin.screenrecorder.contract.RecordVideo
import io.github.japskiddin.screenrecorder.interfaces.ScreenRecorderListener
import io.github.japskiddin.screenrecorder.interfaces.ServiceListener
import io.github.japskiddin.screenrecorder.model.RecordVideoResult
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService.Companion.EXTRA_RECORDER_CODE
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService.Companion.EXTRA_RECORDER_DATA
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService.LocalBinder
import io.github.japskiddin.screenrecorder.utils.isNotLolipop
import java.lang.ref.WeakReference


class ScreenRecorder(
    private val weakReference: WeakReference<Activity>,
    private val listener: ScreenRecorderListener
) : DefaultLifecycleObserver {
    // TODO: Сделать по паттерну Билдер?
    // TODO: Добавить переводы строк
    // TODO: Добавить обработку поворота экрана

    private var isRecording = false
    private var serviceBound = false
    private var screenRecorderService: ScreenRecorderService? = null
    private var recordVideoLauncher: ActivityResultLauncher<Intent>? = null

    init {
        val activity = weakReference.get()
        if (activity is AppCompatActivity) {
            recordVideoLauncher = activity.registerForActivityResult(RecordVideo()) { result ->
                parseRecordIntent(result)
            }
        }
    }

    fun start() {
        val activity = weakReference.get()
        if (activity == null) {
            if (serviceBound) {
                screenRecorderService?.stopRecord()
            } else {
                listener.onStopped()
            }
            return
        }

        if (isNotLolipop()) {
            Toast.makeText(
                activity.applicationContext,
                R.string.err_not_lolipop,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (screenRecorderService == null) {
            val intent = Intent(activity, ScreenRecorderService::class.java)
            ContextCompat.startForegroundService(activity, intent)
            // bind to Service
            activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } else {
            screenRecorderService?.startService()
            screenRecorderService?.startRecord()
        }
    }

    fun stop() {
        if (serviceBound) {
            screenRecorderService?.stopRecord()
        } else {
            listener.onStopped()
        }
    }

    fun isRecording(): Boolean {
        return isRecording
    }

    private fun releaseService(connection: ServiceConnection) {
        serviceBound = false
        screenRecorderService?.setListener(null)
        screenRecorderService = null
        weakReference.get()?.unbindService(connection)
    }

    private fun parseRecordIntent(result: RecordVideoResult) {
        val intent = Intent(weakReference.get(), ScreenRecorderService::class.java)
        intent.putExtra(EXTRA_RECORDER_CODE, result.code)
        intent.putExtra(EXTRA_RECORDER_DATA, result.intent)
        screenRecorderService?.parseIntent(intent)
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
            serviceBound = true
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
            val activity = weakReference.get() ?: return
            if (!activity.isFinishing) {
                listener.onStopped()
                listener.onCompleted(filepath)
                screenRecorderService?.stopService()
            }
        }

        override fun onStartActivity(intent: Intent?) {
            try {
                recordVideoLauncher?.launch(intent)
            } catch (e: ActivityNotFoundException) {
                if (BuildConfig.DEBUG) Log.e(TAG, e.message.toString())
                Toast.makeText(
                    weakReference.get()?.applicationContext,
                    R.string.err_record_activity_not_found,
                    Toast.LENGTH_LONG
                ).show()
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