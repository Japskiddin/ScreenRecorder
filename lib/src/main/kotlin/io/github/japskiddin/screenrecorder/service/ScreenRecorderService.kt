package io.github.japskiddin.screenrecorder.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.japskiddin.screenrecorder.BuildConfig
import io.github.japskiddin.screenrecorder.R
import io.github.japskiddin.screenrecorder.receiver.NotificationReceiver
import io.github.japskiddin.screenrecorder.utils.curSysDate
import io.github.japskiddin.screenrecorder.utils.getRecordingInfo
import io.github.japskiddin.screenrecorder.utils.toBundle
import java.io.File

@Suppress("TooManyFunctions")
internal class ScreenRecorderService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var filePath: String? = null
    private var receiver: ResultReceiver? = null

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Log.d(TAG, "onCreate")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        startAsForeground()
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaProjectionManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            applicationContext.getSystemService(MediaProjectionManager::class.java)
        } else {
            applicationContext.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (BuildConfig.DEBUG) Log.d(TAG, "onDestroy")
        resetAll()
        sendToReceiver(
            Activity.RESULT_OK,
            mapOf(
                EXTRA_ON_MESSAGE_KEY to ON_COMPLETE,
                EXTRA_FILE_PATH_KEY to filePath
            )
        )
        receiver = null
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (BuildConfig.DEBUG) Log.d(TAG, "onStartCommand | $action")

        if (action == null) {
            receiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_BUNDLED_LISTENER, ResultReceiver::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(EXTRA_BUNDLED_LISTENER)
            }
            val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE_KEY, -1)
            val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(EXTRA_RESULT_DATA_KEY, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent?.getParcelableExtra(EXTRA_RESULT_DATA_KEY)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            }

            startAsForeground()

            try {
                initMediaRecorder()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                sendError(GENERAL_ERROR, e)
            }

            try {
                if (resultCode != null && resultData != null) {
                    initMediaProjection(resultCode, resultData)
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                sendError(GENERAL_ERROR, e)
            }

            try {
                initVirtualDisplay()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                sendError(GENERAL_ERROR, e)
            }

            try {
                mediaRecorder.start()
                sendToReceiver(
                    Activity.RESULT_OK,
                    mapOf(EXTRA_ON_MESSAGE_KEY to ON_START)
                )
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                sendError(SETTINGS_ERROR, e)
            }
        } else {
            if (action == ACTION_ATTACH_LISTENER) {
                receiver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_BUNDLED_LISTENER, ResultReceiver::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_BUNDLED_LISTENER)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        if (BuildConfig.DEBUG) Log.d(TAG, "startAsForeground")
        val notification = createNotification()
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
        } catch (e: RemoteException) {
            sendError(GENERAL_ERROR, e)
        } catch (e: SecurityException) {
            sendError(SECURITY_ERROR, e)
        }
    }

    private fun createNotification(): Notification {
        val receiverIntent = Intent(this, NotificationReceiver::class.java)
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, receiverIntent, pendingFlags)

        val action = NotificationCompat.Action.Builder(
            IconCompat.createWithResource(this, android.R.drawable.presence_video_online),
            getString(R.string.stop_record),
            pendingIntent
        ).build()

        return NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_record)
            .setContentTitle(getString(R.string.stop_recording_notification_title))
            .setContentText(getString(R.string.stop_recording_notification_message))
            .addAction(action)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_NONE
        ).apply {
            lightColor = Color.BLUE
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setSound(null, null)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    @Throws(Exception::class)
    private fun initMediaRecorder() {
        val folderPath = File(cacheDir, RECORDER_FOLDER).apply {
            if (!exists()) {
                val created = mkdir()
                if (!created) return
            }
        }.absolutePath

        filePath = "$folderPath/video_$curSysDate.mp4"

        val recordingInfo = getRecordingInfo(this)
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOutputFile(filePath)
            setVideoSize(recordingInfo.width, recordingInfo.height)
            setVideoEncodingBitRate(VIDEO_ENCODING_BITRATE)
            setVideoFrameRate(VIDEO_FRAME_RATE)
            prepare()
        }
    }

    private fun initVirtualDisplay() {
        val displayMetrics = resources.displayMetrics
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            TAG,
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder.surface,
            null,
            null
        )
    }

    private fun initMediaProjection(code: Int, data: Intent) {
        mediaProjection = mediaProjectionManager.getMediaProjection(code, data).apply {
            val callback: MediaProjection.Callback = object : MediaProjection.Callback() {
            }
            registerCallback(callback, null)
        }
    }

    private fun resetAll() {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (ignored: Exception) {
        }
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            mediaRecorder.reset()
            mediaRecorder.release()
            mediaProjection?.stop()
            mediaProjection = null
        } catch (ignored: IllegalStateException) {
        }
    }

    private fun sendError(key: Int, e: Exception) = sendToReceiver(
        Activity.RESULT_CANCELED,
        mapOf(
            EXTRA_ERROR_KEY to key,
            EXTRA_ERROR_REASON_KEY to e.stackTraceToString()
        )
    )

    private fun sendToReceiver(resultCode: Int, map: Map<String, Any?>) =
        receiver?.send(resultCode, map.toBundle())

    internal companion object {
        private val TAG: String = ScreenRecorderService::class.java.simpleName
        private const val NOTIFICATION_CHANNEL_ID = "ScreenRecordChannelId"
        private const val NOTIFICATION_CHANNEL_NAME = "Screen Record Channel"
        private const val RECORDER_FOLDER = "Recordings"
        private const val NOTIFICATION_ID = 101

        const val ACTION_ATTACH_LISTENER: String = "ACTION_ATTACH_LISTENER"

        const val EXTRA_BUNDLED_LISTENER: String = "EXTRA_BUNDLED_LISTENER"
        const val EXTRA_ERROR_REASON_KEY: String = "EXTRA_ERROR_REASON_KEY"
        const val EXTRA_ERROR_KEY: String = "EXTRA_ERROR_KEY"
        const val EXTRA_FILE_PATH_KEY: String = "EXTRA_FILE_PATH_KEY"
        const val EXTRA_ON_MESSAGE_KEY: String = "EXTRA_ON_MESSAGE_KEY"
        const val EXTRA_RESULT_CODE_KEY: String = "EXTRA_RESULT_CODE_KEY"
        const val EXTRA_RESULT_DATA_KEY: String = "EXTRA_RESULT_DATA_KEY"

        const val GENERAL_ERROR: Int = -100
        const val SETTINGS_ERROR: Int = -101
        const val SECURITY_ERROR: Int = -102
        const val ON_START: Int = 100
        const val ON_COMPLETE: Int = 101

        private const val VIDEO_ENCODING_BITRATE = 512 * 3000
        private const val VIDEO_FRAME_RATE = 30
    }
}
