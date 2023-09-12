package io.github.japskiddin.screenrecorder.interfaces

import android.content.Intent

interface ServiceListener {
    fun onRecordStarted()
    fun onRecordStopped(filepath: String?)
    fun onStartActivity(intent: Intent?)
    fun onServiceStopped()
}