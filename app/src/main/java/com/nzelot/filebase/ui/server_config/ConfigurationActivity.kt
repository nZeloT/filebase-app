package com.nzelot.filebase.ui.server_config

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import com.nzelot.filebase.R
import com.nzelot.filebase.ui.main_content.MainActivity
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "org.nzelot.filebase.ConfigurationActivity"

@AndroidEntryPoint
class ConfigurationActivity : AppCompatActivity() {

    private val viewModel : ConfigurationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)
        setTitle(R.string.server_configuration)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val editTextHostName = findViewById<EditText>(R.id.editTextServerAddress)
        val editTextUsername  = findViewById<EditText>(R.id.editTextUsername)
        val editTextWorkgroup = findViewById<EditText>(R.id.editTextWorkgroup)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val editTextShareName = findViewById<EditText>(R.id.editTextShareName)
        val buttonTestConnection = findViewById<Button>(R.id.buttonTestServerConnection)
        val textTestResult = findViewById<TextView>(R.id.textViewTestServerConnectionResult)
        val textTestResultExplain = findViewById<TextView>(R.id.textViewTestServerConnectionErrorExplanation)
        val buttonNext = findViewById<Button>(R.id.buttonSaveConfiguration)

        viewModel.state.observe(this) {
            buttonNext.isEnabled = it.isSuccessfullyTested && !it.isTestOngoing
            buttonTestConnection.isEnabled = it.isTestable && !it.isTestOngoing
            Log.d(TAG, "${it.isTestable} && !${it.isTestOngoing}")
            textTestResult.isVisible = it.isTestCompleted && !it.isTestOngoing
            textTestResultExplain.isVisible = it.isTestCompleted && !it.isTestOngoing

            editTextHostName.isEnabled = !it.isTestOngoing
            editTextUsername.isEnabled = !it.isTestOngoing
            editTextWorkgroup.isEnabled = !it.isTestOngoing
            editTextPassword.isEnabled = !it.isTestOngoing
            editTextShareName.isEnabled = !it.isTestOngoing

            textTestResult.text = it.testResultMessage
            textTestResultExplain.text = it.testResultExplanation
        }

        buttonTestConnection.setOnClickListener {
            viewModel.testConnection()
        }

        buttonNext.setOnClickListener {
            viewModel.storeConfig()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        resetOnEdit("Hostname", editTextHostName, viewModel.hostname)
        resetOnEdit("Workgroup", editTextWorkgroup, viewModel.workgroup)
        resetOnEdit("Username", editTextUsername, viewModel.username)
        resetOnEdit("Sharename", editTextShareName, viewModel.shareName)

        editTextPassword.doAfterTextChanged {
            Log.d(TAG, "doAfterTextChanged for Password")
            viewModel.invalidateTestResult()
            viewModel.password.value = it.toString().toCharArray()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when(item.itemId) {
            android.R.id.home -> {
                finish();
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun resetOnEdit(fieldName: String, field: EditText, model: MutableLiveData<String>) {
        field.doAfterTextChanged {
            Log.d(TAG, "doAfterTextChanged for $fieldName")
            if(it.toString() != model.value!!) {
                val currentVal = model.value!!
                val newValue = it.toString()
                Log.i(TAG, "Received new $fieldName; Changing from '$currentVal' to '$newValue'")
                viewModel.invalidateTestResult()
                model.value = newValue
            }
        }
    }
}