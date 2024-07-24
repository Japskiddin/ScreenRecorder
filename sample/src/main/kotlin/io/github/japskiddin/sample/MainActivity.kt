package io.github.japskiddin.sample

import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import io.github.japskiddin.sample.databinding.ActivityMainBinding
import io.github.japskiddin.screenrecorder.ScreenRecorder
import io.github.japskiddin.screenrecorder.ScreenRecorder.ScreenRecorderListener

class MainActivity : AppCompatActivity() {
  private lateinit var appBarConfiguration: AppBarConfiguration
  private lateinit var binding: ActivityMainBinding
  private lateinit var screenRecorder: ScreenRecorder

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean? ->
    if (isGranted != null && isGranted) {
      prepareScreenRecord()
    }
  }

  private val mediaProjectionLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result: ActivityResult ->
    val data = result.data
    val resultCode = result.resultCode
    if (data == null) return@registerForActivityResult
    screenRecorder.startScreenRecording(resultCode, data)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)

    val navController = findNavController(R.id.nav_host_fragment_content_main)
    appBarConfiguration = AppBarConfiguration(navController.graph)
    setupActionBarWithNavController(navController, appBarConfiguration)

    binding.fab.setOnClickListener { onRecordClick() }

    screenRecorder = ScreenRecorder(this)
    screenRecorder.setListener(screenRecorderListener)

    if (savedInstanceState != null) {
      restoreScreenRecordService()
    }
  }

  override fun onDestroy() {
    screenRecorder.setListener(null)
    super.onDestroy()
  }

  private fun restoreScreenRecordService() {
    if (screenRecorder.isBusyRecording) {
      enableRecord()
    }
  }

  private fun onRecordClick() {
    if (screenRecorder.isBusyRecording) {
      screenRecorder.stopScreenRecording()
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
          ) == PackageManager.PERMISSION_GRANTED
        ) {
          prepareScreenRecord()
        } else {
          requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
      } else {
        prepareScreenRecord()
      }
    }
  }

  private fun enableRecord() {
    val animation = AlphaAnimation(1f, .1f).apply {
      interpolator = LinearInterpolator()
      duration = 1000L
      repeatMode = Animation.REVERSE
      repeatCount = Animation.INFINITE
    }
    binding.fab.startAnimation(animation)
  }

  private fun disableRecord() {
    binding.fab.clearAnimation()
  }

  private fun prepareScreenRecord() {
    val mediaProjectionManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      getSystemService(MediaProjectionManager::class.java)
    } else {
      getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }
    mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
  }

  private val screenRecorderListener = object : ScreenRecorderListener {
    override fun recorderOnStart() {
      if (isFinishing) return
      Toast.makeText(applicationContext, "Recording started", Toast.LENGTH_SHORT).show()
      enableRecord()
    }

    override fun recorderOnError(errorMessage: Int) {
      if (isFinishing) return
      Toast.makeText(applicationContext, "Recording failed with error", Toast.LENGTH_SHORT)
        .show()
      disableRecord()
    }

    override fun recorderOnComplete(filepath: String?) {
      if (isFinishing) return
      Toast.makeText(
        applicationContext,
        "Recording completed\nVideo saved to: $filepath",
        Toast.LENGTH_SHORT
      ).show()
      disableRecord()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    return when (item.itemId) {
      R.id.action_settings -> true
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    val navController = findNavController(R.id.nav_host_fragment_content_main)
    return navController.navigateUp(appBarConfiguration)
      || super.onSupportNavigateUp()
  }
}
