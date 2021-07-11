package com.nzelot.filebase.ui.main_content

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.nzelot.filebase.R
import com.nzelot.filebase.ui.server_config.ConfigurationActivity
import com.nzelot.filebase.worker.SMBTransferWorker
import dagger.hilt.android.AndroidEntryPoint
import java.time.format.DateTimeFormatter

private const val TAG = "org.nzelot.filebase.MainActivity"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainContentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(applicationContext, "Required Permission Granted!", Toast.LENGTH_SHORT).show()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 42)
        }

        val textViewCurrentSMBShareAddress = findViewById<TextView>(R.id.textViewCurrentServerAddressValue)
        val textViewCurrentSMBUsername = findViewById<TextView>(R.id.textViewCurrentServerUsername)
        val textViewCurrentLastSync = findViewById<TextView>(R.id.textViewCurrentServerLastSyncValue)

        val buttonActionSyncNow = findViewById<Button>(R.id.buttonSyncNow)
        val buttonRefreshConnectState = findViewById<ImageButton>(R.id.buttonRefreshConnectState)
        val textViewActionConnected = findViewById<TextView>(R.id.textViewSyncNowConnectedStatus)

        val textViewStatusLog = findViewById<TextView>(R.id.textViewStatusLog)

        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        viewModel.actionState.observe(this) {
            buttonActionSyncNow.isEnabled = it.isServerConnected && !it.isSyncOngoing
            textViewActionConnected.text = if (it.isServerConnected) {
                "Connected"
            } else {
                "Not Connected"
            }
        }

        viewModel.config.observe(this) {
            textViewCurrentSMBShareAddress.text = it.address
            textViewCurrentSMBUsername.text = it.username
            textViewCurrentLastSync.text = it.lastSync.format(dateTimeFormatter)
        }

        buttonRefreshConnectState.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    val uploadWorker: WorkRequest = OneTimeWorkRequestBuilder<SMBTransferWorker>().build()
                    WorkManager.getInstance(applicationContext).enqueue(uploadWorker)
                }
                else -> {
                    Toast.makeText(applicationContext, "Missing required permission!", Toast.LENGTH_LONG).show()
                }
            }
        }

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
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        return
    }
}