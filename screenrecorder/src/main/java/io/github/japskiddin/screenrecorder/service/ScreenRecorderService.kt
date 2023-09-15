package io.github.japskiddin.screenrecorder.service

import android.annotation.TargetApi
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.japskiddin.screenrecorder.BuildConfig
import io.github.japskiddin.screenrecorder.R
import io.github.japskiddin.screenrecorder.interfaces.ServiceListener
import io.github.japskiddin.screenrecorder.utils.getRecordingInfo
import io.github.japskiddin.screenrecorder.utils.getSysDate
import io.github.japskiddin.screenrecorder.utils.showToast
import java.io.File
import java.io.IOException

// TODO: подумать над неймингом методов, не нравится

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenRecorderService : Service() {
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var mediaProjectionManager: MediaProjectionManager

    /**
     * Binder given to clients
     */
    private val binder: IBinder = LocalBinder()

    /**
     * Registered callbacks
     */
    private var serviceListener: ServiceListener? = null

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var filePath: String? = null
    private var isServiceAlive = false

    // Class used for the client Binder.
    inner class LocalBinder : Binder() {
        // Return this instance of MyService so clients can call public methods
        val service: ScreenRecorderService get() = this@ScreenRecorderService
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Log.d(TAG, "onCreate")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        createNotification()
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        isServiceAlive = true
    }

    override fun onDestroy() {
        if (BuildConfig.DEBUG) Log.d(TAG, "onDestroy")
        reset()
        release()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (BuildConfig.DEBUG) Log.d(TAG, "onStartCommand | " + intent.action)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        if (BuildConfig.DEBUG) Log.d(TAG, "onBind")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (BuildConfig.DEBUG) Log.d(TAG, "onUnbind")
        return true
    }

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
    }

    fun setListener(listener: ServiceListener?) {
        serviceListener = listener
    }

    fun stopRecord() {
        reset()
        serviceListener?.onRecordStopped(filePath)
    }

    fun startRecord() {
        if (mediaProjection == null) {
            try {
                val intent = Intent().apply {
                    putExtra(
                        EXTRA_RECORDER_DATA,
                        mediaProjectionManager.createScreenCaptureIntent()
                    )
                }
                serviceListener?.onStartActivity(intent)
            } catch (e: ActivityNotFoundException) {
                if (BuildConfig.DEBUG) Log.e(TAG, e.message.toString())
                showToast(applicationContext, R.string.err_screen_record)
            }
            return
        }

        val prepared = prepare()
        if (prepared) {
            virtualDisplay = getVirtualDisplay()
            if (virtualDisplay == null) {
                stop()
                return
            }

            try {
                mediaRecorder.start()
            } catch (e: IllegalStateException) {
                if (BuildConfig.DEBUG) Log.e(TAG, e.message.toString())
                stop()
                return
            }
            serviceListener?.onRecordStarted()
        } else {
            reset()
            stop()
        }
    }

    fun startService() {
        if (!isServiceAlive) {
            isServiceAlive = true
            createNotification()
        }
    }

    fun stopService() {
        if (isServiceAlive) {
            stop()
        }
    }

    fun parseIntent(intent: Intent) {
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RECORDER_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RECORDER_DATA)
        }
        val code = intent.getIntExtra(EXTRA_RECORDER_CODE, 0)
        if (code != Activity.RESULT_OK || data == null) {
            stop()
        } else {
            mediaProjection = mediaProjectionManager.getMediaProjection(code, data)
            startRecord()
        }
    }

    private fun stop() {
        if (BuildConfig.DEBUG) Log.d(TAG, "OnStopService")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (BuildConfig.DEBUG) Log.d(TAG, "stopForeground")
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
        serviceListener?.onServiceStopped()
        isServiceAlive = false
    }

    private fun createNotification() {
        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setSmallIcon(R.drawable.ic_videocam)
            .build()

        if (BuildConfig.DEBUG) Log.d(TAG, "startForeground")
        startForeground(NOTIFICATION_ID, notification)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setSound(null, null)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    private fun prepare(): Boolean {
        val folder = File(cacheDir, RECORDER_FOLDER).apply {
            if (!exists()) {
                val created = mkdir()
                if (!created) {
                    showToast(applicationContext, R.string.err_create_recordings_dir)
                    return false
                }
            }
        }

        val videoName = "video_${getSysDate()}.mp4"
        filePath = folder.absolutePath + File.separator + videoName
        val recordingInfo = getRecordingInfo(this)

        mediaRecorder.apply {
            try {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setOutputFile(filePath)
                setVideoSize(recordingInfo.width, recordingInfo.height)
                setVideoEncodingBitRate(512 * 3000)
                setVideoFrameRate(30)
                // TODO добавить максимальный размер файла
                prepare()
                return true
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, e.message.toString())
                when (e) {
                    is IllegalStateException,
                    is IOException -> showToast(applicationContext, R.string.err_screen_record)

                    else -> showToast(applicationContext, R.string.err_unknown)
                }
                return false
            }
        }
    }

    private fun getVirtualDisplay(): VirtualDisplay? {
        val displayMetrics = resources.displayMetrics
        val screenDensity = displayMetrics!!.densityDpi
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        return mediaProjection?.createVirtualDisplay(
            this.javaClass.simpleName,
            width,
            height,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder.surface,
            null,
            null
        )
    }

    private fun reset() {
        try {
            mediaRecorder.stop()
            mediaRecorder.reset()
            virtualDisplay?.release()
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: RuntimeException) {
            if (BuildConfig.DEBUG) Log.e(TAG, e.message.toString())
        }
    }

    private fun release() {
        try {
            mediaRecorder.release()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, e.message.toString())
        }
    }

    companion object {
        private val TAG = ScreenRecorderService::class.java.simpleName
        const val RECORDER_FOLDER = "Recordings"
        const val EXTRA_RECORDER_CODE = "RECORDER_CODE"
        const val EXTRA_RECORDER_DATA = "RECORDER_DATA"
        const val NOTIFICATION_CHANNEL_ID = "RecordScreenChannel"
        const val NOTIFICATION_ID = 101
    }
}