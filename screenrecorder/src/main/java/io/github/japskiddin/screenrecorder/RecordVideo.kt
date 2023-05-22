package io.github.japskiddin.screenrecorder

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import io.github.japskiddin.screenrecorder.ScreenRecorderService.Companion.EXTRA_RECORDER_DATA

class RecordVideo : ActivityResultContract<Intent, Pair<Int, Intent?>>() {
    override fun createIntent(context: Context, input: Intent): Intent {
        val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            input.getParcelableExtra(EXTRA_RECORDER_DATA, Intent::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            input.getParcelableExtra(EXTRA_RECORDER_DATA)!!
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Pair<Int, Intent?> {
        return Pair(resultCode, intent)
    }
}