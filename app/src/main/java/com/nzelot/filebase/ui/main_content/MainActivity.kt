package com.nzelot.filebase.ui.main_content

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nzelot.filebase.R
import com.nzelot.filebase.databinding.ActivityMainBinding
import com.nzelot.filebase.ui.server_config.ConfigurationActivity
import com.nzelot.filebase.worker.FileCheckWorker
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "org.nzelot.filebase.MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainContentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        val btnSyncNow = findViewById<Button>(R.id.buttonSyncNow)
        btnSyncNow.setOnClickListener {
            startSyncNow()
        }

        val btnCheckShareAvail = findViewById<Button>(R.id.buttonRefreshShareAvailable)
        btnCheckShareAvail.setOnClickListener{
            checkShareAvailable()
        }

        requestPermissionOrDo()
    }

    private fun startSyncNow() {
        Log.d(TAG, "Sync triggered")
        requestPermissionOrDo(this::launchSyncOnce)
    }

    private fun checkShareAvailable() {
        viewModel.checkShareAvailability()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.actionConfigureServer -> {
            val intent = Intent(this, ConfigurationActivity::class.java)
            startActivity(intent)
            true
        }
        R.id.actionRequestPermissions -> {
            requestPermissionOrDo()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        return
    }

    private fun requestPermissionOrDo(
        cb: () -> Unit = {
            Toast.makeText(
                applicationContext,
                "Required Permission Granted!",
                Toast.LENGTH_SHORT
            ).show()
        }
    ) {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cb()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 42)
        }
    }

    private fun launchSyncOnce() {
        Log.d(TAG, "Enqueueing FileCheckWorker")
        val work = OneTimeWorkRequestBuilder<FileCheckWorker>().build()
        WorkManager.getInstance(applicationContext).enqueue(work)
    }
}