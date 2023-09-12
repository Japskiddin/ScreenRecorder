package io.github.japskiddin.screenrecorder.interfaces

interface ScreenRecorderListener {
    fun onStarted()
    fun onStopped()
    fun onCompleted(filepath: String?)
}