package com.nzelot.filebase.ui.server_config

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import com.nzelot.filebase.R
import com.nzelot.filebase.ui.main_content.MainActivity
import com.nzelot.filebase.worker.FileCheckWorker
import com.nzelot.filebase.worker.SMBTransferWorker
import dagger.hilt.android.AndroidEntryPoint
import java.lang.ClassCastException
import java.lang.IllegalStateException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private const val TAG = "org.nzelot.filebase.ConfigurationActivity"

@AndroidEntryPoint
class ConfigurationActivity : AppCompatActivity(), DatePickingDialog.OnValueSetListener {

    private val viewModel: ConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)
        setTitle(R.string.server_configuration)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val editTextHostName = findViewById<EditText>(R.id.editTextServerAddress)
        val editTextUsername = findViewById<EditText>(R.id.editTextUsername)
        val editTextWorkgroup = findViewById<EditText>(R.id.editTextWorkgroup)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val editTextShareName = findViewById<EditText>(R.id.editTextShareName)
        val textViewStartSyncDate = findViewById<TextView>(R.id.textViewStartSyncDate)
        val buttonTestConnection = findViewById<Button>(R.id.buttonTestServerConnection)
        val textTestResult = findViewById<TextView>(R.id.textViewTestServerConnectionResult)
        val textTestResultExplain =
            findViewById<TextView>(R.id.textViewTestServerConnectionErrorExplanation)
        val buttonSave = findViewById<Button>(R.id.buttonSaveConfiguration)

        viewModel.state.observe(this) {
            buttonSave.isEnabled = it.isStoreable
            buttonTestConnection.isEnabled = it.isTestable && !it.isTestOngoing
            Log.d(TAG, "${it.isTestable} && !${it.isTestOngoing}")
            textTestResult.isVisible = it.isTestCompleted && !it.isTestOngoing
            textTestResultExplain.isVisible = it.isTestCompleted && !it.isTestOngoing

            editTextHostName.isEnabled = !it.isTestOngoing
            editTextUsername.isEnabled = !it.isTestOngoing
            editTextWorkgroup.isEnabled = !it.isTestOngoing
            editTextPassword.isEnabled = !it.isTestOngoing
            editTextShareName.isEnabled = !it.isTestOngoing
            textViewStartSyncDate.isEnabled = !it.isTestOngoing

            textTestResult.text = it.testResultMessage
            textTestResultExplain.text = it.testResultExplanation
        }

        viewModel.syncStart.observe(this) {
            Log.d(TAG, "syncStart changed")
            textViewStartSyncDate.text = it.format(DateTimeFormatter.ISO_DATE)
        }

        buttonTestConnection.setOnClickListener {
            viewModel.testConnection()
        }

        buttonSave.setOnClickListener {
            Log.i(TAG, "Stopping all current background worker.")
            stopPeriodicFileChecker()
            viewModel.storeConfig()
            Log.i(TAG, "Rescheduling background worker")
            startPeriodicFileChecker()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        initFieldWith("Hostname", viewModel.hostname.value!!, editTextHostName)
        initFieldWith("Workgroup", viewModel.workgroup.value!!, editTextWorkgroup)
        initFieldWith("Username", viewModel.username.value!!, editTextUsername)
        initFieldWith("Sharename", viewModel.shareName.value!!, editTextShareName)

        resetOnEdit("Hostname", editTextHostName, viewModel.hostname)
        resetOnEdit("Workgroup", editTextWorkgroup, viewModel.workgroup)
        resetOnEdit("Username", editTextUsername, viewModel.username)
        resetOnEdit("Sharename", editTextShareName, viewModel.shareName)

        editTextPassword.doAfterTextChanged {
            Log.d(TAG, "doAfterTextChanged for Password")
            viewModel.invalidateTestResult()
            viewModel.password.value = it.toString().toCharArray()
        }

        textViewStartSyncDate.setOnClickListener {
            Log.d(TAG, "OnClick Sync Start Date")
            var zdt = viewModel.syncStart.value!!
            if (zdt.isEqual(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())))
                zdt = ZonedDateTime.now(ZoneId.systemDefault())

            val dialog = DatePickingDialog(zdt)
            dialog.show(supportFragmentManager, "StartSyncDatePicker")
        }

    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun stopPeriodicFileChecker() {
        val wm = WorkManager.getInstance(applicationContext)
        wm.cancelUniqueWork(FileCheckWorker.CHECKER_TAG)
        wm.cancelAllWorkByTag(FileCheckWorker.CHECKER_TAG) //to also cancel manually triggered work
        wm.cancelAllWorkByTag(SMBTransferWorker.UPLOAD_TAG) // also cancel uploads
    }

    private fun startPeriodicFileChecker() {
        //Create the upload worker
        val work = PeriodicWorkRequestBuilder<FileCheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    .build()
            )
            .addTag(FileCheckWorker.CHECKER_TAG)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                FileCheckWorker.CHECKER_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )
    }

    override fun newDateValue(dialog: DialogFragment, year: Int, month: Int, dayOfMonth: Int) {
        Log.d(TAG, "Received new Date picked by user")
        viewModel.updateSyncDate(year, month, dayOfMonth)
    }

    private fun resetOnEdit(fieldName: String, field: EditText, model: MutableLiveData<String>) {
        field.doAfterTextChanged {
            Log.d(TAG, "doAfterTextChanged for $fieldName")
            if (it.toString() != model.value!!) {
                val currentVal = model.value!!
                val newValue = it.toString()
                Log.i(TAG, "Received new $fieldName; Changing from '$currentVal' to '$newValue'")
                viewModel.invalidateTestResult()
                model.value = newValue
            }
        }
    }

    private fun <T : CharSequence> initFieldWith(
        fieldName: String,
        value: T,
        field: TextView
    ) {
        Log.d(TAG, "Initializing $fieldName")
        field.text = value
    }
}

class DatePickingDialog(
    private val zdt: ZonedDateTime
) : DialogFragment(), DatePickerDialog.OnDateSetListener {

    private lateinit var listener: OnValueSetListener

    interface OnValueSetListener {
        fun newDateValue(dialog: DialogFragment, year: Int, month: Int, dayOfMonth: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            DatePickerDialog(it, this, zdt.year, zdt.monthValue - 1, zdt.dayOfMonth)
        } ?: throw IllegalStateException("Activity can't be null!")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as OnValueSetListener
        } catch (ex: ClassCastException) {
            throw IllegalStateException("$context must implement Listener!")
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        listener.newDateValue(this, year, month + 1, dayOfMonth)
    }
}