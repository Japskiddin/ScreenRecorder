package io.github.japskiddin.sample

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import io.github.japskiddin.sample.databinding.ActivityMainBinding
import io.github.japskiddin.screenrecorder.ScreenRecorder
import io.github.japskiddin.screenrecorder.interfaces.ScreenRecorderListener

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var screenRecorder: ScreenRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            if (screenRecorder.isRecording()) {
                screenRecorder.stop()
            } else {
                screenRecorder.start()
            }
        }

        screenRecorder = ScreenRecorder(this, listener)
        screenRecorder.restoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        screenRecorder.saveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    private val listener = object : ScreenRecorderListener {
        override fun onStarted() {
            Toast.makeText(applicationContext, "Recording started", Toast.LENGTH_SHORT).show()
        }

        override fun onStopped() {
            Toast.makeText(applicationContext, "Recording stopped", Toast.LENGTH_SHORT).show()
        }

        override fun onRestored() {
            Toast.makeText(applicationContext, "Recording restored", Toast.LENGTH_SHORT).show()
        }

        override fun onCompleted(filepath: String?) {
            Toast.makeText(
                applicationContext,
                "Recording completed\nVideo saved to: $filepath",
                Toast.LENGTH_SHORT
            ).show()
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