package io.github.japskiddin.screenrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, ScreenRecorderService::class.java)
        context.stopService(service)
    }
}