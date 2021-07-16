package com.nzelot.filebase.ui.main_content

import android.util.Log
import androidx.lifecycle.*
import com.nzelot.filebase.data.model.CurrentSMBConfiguration
import com.nzelot.filebase.data.model.CurrentSMBState
import com.nzelot.filebase.data.repository.SMBConfigurationRepository
import com.nzelot.filebase.data.repository.SMBStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "org.nzelot.filebase.MainContentViewModel"

@HiltViewModel
class MainContentViewModel @Inject constructor(
    private val smbConfigurationRepository: SMBConfigurationRepository,
    private val smbStateRepository: SMBStateRepository
) : ViewModel() {

    private val _config = MutableLiveData<CurrentSMBConfiguration>()
    val config : LiveData<CurrentSMBConfiguration> = _config

    private val _state = MutableLiveData<CurrentSMBState>()
    val state : LiveData<CurrentSMBState> = _state

    val lastSync : LiveData<String> = Transformations.map(_state) {
        it.lastSyncZdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    init {
        Log.e(TAG, "init of viewModel")

        viewModelScope.launch {
            _config.value = smbConfigurationRepository.currentConfiguration
            Log.i(TAG, "Received initial config of ${_config.value}")

            _state.value = smbStateRepository.state
            Log.i(TAG, "Received initial state of ${_state.value}")
        }
    }

    fun startSyncNow() {
        Log.d(TAG, "Sync was triggered actively")
    }

}