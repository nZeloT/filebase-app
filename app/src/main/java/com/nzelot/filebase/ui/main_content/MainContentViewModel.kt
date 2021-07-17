package com.nzelot.filebase.ui.main_content

import android.util.Log
import androidx.lifecycle.*
import com.nzelot.filebase.SMB
import com.nzelot.filebase.data.Result
import com.nzelot.filebase.data.model.CurrentSMBConfiguration
import com.nzelot.filebase.data.repository.SMBConfigurationRepository
import com.nzelot.filebase.data.repository.SMBStateRepository
import com.nzelot.filebase.data.repository.StatusLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "org.nzelot.filebase.MainContentViewModel"

@HiltViewModel
class MainContentViewModel @Inject constructor(
    private val smbConfigurationRepository: SMBConfigurationRepository,
    private val smbStateRepository: SMBStateRepository,
    log : StatusLogRepository
) : ViewModel() {

    private val _config = MutableLiveData<CurrentSMBConfiguration>()
    val config : LiveData<CurrentSMBConfiguration> = _config

    private val _lastSync = smbStateRepository.lastSyncDate.asLiveData()
    val lastSync : LiveData<String> = Transformations.map(_lastSync) {
        it.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    val isSyncOngoing = smbStateRepository.isSyncOngoing.asLiveData()

    val isShareAvailable = smbStateRepository.isShareAvailable.asLiveData()

    private val _statusLog = log.logs.asLiveData()
    val statusLog : LiveData<String> = Transformations.map(_statusLog) {
        it.map { entry ->
            "${entry.severity.short()}: ${entry.message}"
        }.fold(StringBuilder()) { sb, s ->
            sb.appendLine(s)
        }.toString()
    }

    init {
        Log.e(TAG, "init of viewModel")

        viewModelScope.launch {
            _config.value = smbConfigurationRepository.currentConfiguration
            Log.i(TAG, "Received initial config of ${_config.value}")
        }
    }

    fun checkShareAvailability() {
        viewModelScope.launch {
            val newState = when(val r = SMB.testShareLogin(smbConfigurationRepository.config)){
                is Result.Error -> {
                    Log.e(TAG, "Failed to connect to share.", r.exception)
                    false
                }
                is Result.Success -> true
            }
            Log.d(TAG, "isShareAvailable => $newState")
            smbStateRepository.updateShareAvailable(newState)
        }
    }
}