package io.github.japskiddin.screenrecorder

interface ScreenRecorderListener {
    fun recorderOnStart()

    fun recorderOnComplete(filepath: String?)

    fun recorderOnError(errorMessage: Int)
}
