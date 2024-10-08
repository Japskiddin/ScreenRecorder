package io.github.japskiddin.screenrecorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.japskiddin.screenrecorder.service.ScreenRecorderService

internal class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, ScreenRecorderService::class.java)
        context.stopService(service)
    }
}
