package com.nzelot.filebase.ui.main_content

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nzelot.filebase.data.repository.SMBConfigurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "org.nzelot.filebase.MainContentViewModel"

@HiltViewModel
class MainContentViewModel @Inject constructor(
    private val smbConfigurationRepository: SMBConfigurationRepository,
) : ViewModel() {

    private val _actionState = MutableLiveData<ActionsState>()
    val actionState : LiveData<ActionsState> = _actionState

    private val _config = MutableLiveData<CurrentSMBConfiguration>()
    val config : LiveData<CurrentSMBConfiguration> = _config

    init {
        Log.e(TAG, "init of viewModel")

        viewModelScope.launch {
            _actionState.value = ActionsState(
                isServerConnected = false,
                isSyncOngoing = false
            )

            _config.value = smbConfigurationRepository.currentConfiguration
            Log.i(TAG, "Received initial state of ${_config.value}")
        }
    }

    fun testConfiguration() {
        viewModelScope.launch {
        }
    }

}