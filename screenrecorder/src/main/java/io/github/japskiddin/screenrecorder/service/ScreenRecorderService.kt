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
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import io.github.japskiddin.screenrecorder.BuildConfig
import io.github.japskiddin.screenrecorder.R
import io.github.japskiddin.screenrecorder.utils.getRecordingInfo
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ScreenRecorderService : Service() {
    interface ServiceListener {
        fun onRecordStarted()
        fun onRecordStopped(filepath: String?)
        fun onStartActivity(intent: Intent?)
        fun onServiceStopped()
    }

    /*
    Binder given to clients
     */
    private val binder: IBinder = LocalBinder()

    /*
    Registered callbacks
     */
    private var serviceListener: ServiceListener? = null

    private var displayMetrics: DisplayMetrics? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var filePath: String? = null
    private var isRunning = false

    // Class used for the client Binder.
    inner class LocalBinder : Binder() {
        // Return this instance of MyService so clients can call public methods
        val service: ScreenRecorderService get() = this@ScreenRecorderService
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Log.d(TAG, "onCreate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        createNotification()
        displayMetrics = resources.displayMetrics
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        isRunning = true
    }

    override fun onBind(intent: Intent): IBinder {
        if (BuildConfig.DEBUG) Log.d(TAG, "onBind");
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (BuildConfig.DEBUG) Log.d(TAG, "onUnbind");
        return true
    }

    override fun onRebind(intent: Intent) {
        super.onRebind(intent)
    }

    fun setListener(listener: ServiceListener?) {
        serviceListener = listener
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (BuildConfig.DEBUG) Log.d(TAG, "onStartCommand | " + intent.getAction());
        return START_NOT_STICKY
    }

    private fun onStopService() {
        if (BuildConfig.DEBUG) Log.d(TAG, "OnStopService");
        isRunning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (BuildConfig.DEBUG) Log.d(TAG, "stopForeground");
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
        if (serviceListener != null) {
            serviceListener!!.onServiceStopped()
        }
    }

    override fun onDestroy() {
        release()
        if (BuildConfig.DEBUG) Log.d(TAG, "onDestroy");
        super.onDestroy()
    }

    private fun createNotification() {
        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setSmallIcon(R.drawable.ic_videocam)
            .build()

        if (BuildConfig.DEBUG) Log.d(TAG, "startForeground");
        startForeground(101, notification)
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
        val directory = cacheDir.toString() + File.separator + RECORDER_FOLDER
        val folder = File(directory)
        var success = true
        if (!folder.exists()) {
            success = folder.mkdir()
        }
        filePath = if (success) {
            val videoName = "video_$curSysDate.mp4"
            directory + File.separator + videoName
        } else {
            Toast.makeText(
                applicationContext,
                R.string.err_create_recordings_dir,
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        val recordingInfo = getRecordingInfo(this)
        val width = recordingInfo?.width ?: displayMetrics!!.widthPixels
        val height = recordingInfo?.height ?: displayMetrics!!.heightPixels
        try {
            mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder!!.setOutputFile(filePath)
            mediaRecorder!!.setVideoSize(width, height)
            mediaRecorder!!.setVideoEncodingBitRate(512 * 3000)
            mediaRecorder!!.setVideoFrameRate(30)
            // TODO добавить максимальный размер файла
            mediaRecorder!!.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
            when (e) {
                is IllegalStateException,
                is IOException -> Toast.makeText(
                    applicationContext,
                    R.string.err_screen_record,
                    Toast.LENGTH_LONG
                ).show()

                else -> Toast.makeText(applicationContext, R.string.err_unknown, Toast.LENGTH_LONG)
                    .show()
            }
            return false
        }
        return true
    }

    private val curSysDate: String
        get() = SimpleDateFormat("MM-dd_HH-mm", Locale.getDefault()).format(Date())

    private fun getVirtualDisplay(): VirtualDisplay {
        val screenDensity = displayMetrics!!.densityDpi
        val width = displayMetrics!!.widthPixels
        val height = displayMetrics!!.heightPixels
        return mediaProjection!!.createVirtualDisplay(
            this.javaClass.simpleName, width, height,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder!!.surface, null, null
        )
    }

    fun stopRecord() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder!!.stop()
                mediaRecorder!!.reset()
            }
            if (virtualDisplay != null) {
                virtualDisplay!!.release()
            }
            if (mediaProjection != null) {
                mediaProjection!!.stop()
                mediaProjection = null
            }
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        if (serviceListener != null) {
            serviceListener!!.onRecordStopped(filePath)
        }
    }

    fun startRecord() {
        if (mediaProjection == null) {
            try {
                val intent = Intent()
                intent.putExtra(
                    EXTRA_RECORDER_DATA,
                    mediaProjectionManager!!.createScreenCaptureIntent()
                )
                if (serviceListener != null) {
                    serviceListener!!.onStartActivity(intent)
                }
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, R.string.err_screen_record, Toast.LENGTH_LONG)
                    .show()
            }
            return
        }
        val prepared = prepare()
        if (prepared) {
            virtualDisplay = getVirtualDisplay()
            try {
                mediaRecorder!!.start()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                onStopService()
                return
            }
            if (serviceListener != null) {
                serviceListener!!.onRecordStarted()
            }
        } else {
            try {
                if (mediaRecorder != null) {
                    mediaRecorder!!.reset()
                }
                if (virtualDisplay != null) {
                    virtualDisplay!!.release()
                }
                if (mediaProjection != null) {
                    mediaProjection!!.stop()
                    mediaProjection = null
                }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            onStopService()
        }
    }

    fun release() {
        if (mediaRecorder != null) {
            mediaRecorder!!.release()
        }
    }

    fun startService() {
        if (!isRunning) {
            isRunning = true
            createNotification()
        }
    }

    fun stopService() {
        if (isRunning) {
            onStopService()
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
        if (code != Activity.RESULT_OK) {
            onStopService()
        } else {
            if (data != null) {
                mediaProjection = mediaProjectionManager!!.getMediaProjection(
                    code,
                    data
                )
            }
            startRecord()
        }
    }

    companion object {
        private val TAG = ScreenRecorderService::class.java.simpleName
        const val RECORDER_FOLDER = "Recordings"
        const val EXTRA_RECORDER_CODE = "RECORDER_CODE"
        const val EXTRA_RECORDER_DATA = "RECORDER_DATA"
        const val NOTIFICATION_CHANNEL_ID = "RecordScreenChannel"
    }
}