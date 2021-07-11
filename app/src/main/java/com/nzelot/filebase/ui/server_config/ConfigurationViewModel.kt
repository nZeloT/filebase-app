package com.nzelot.filebase.ui.server_config

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nzelot.filebase.data.Result
import com.nzelot.filebase.data.model.Credentials
import com.nzelot.filebase.data.model.SMBConfiguration
import com.nzelot.filebase.SMB
import com.nzelot.filebase.data.repository.SMBConfigurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "org.nzelot.filebase.ServerConfigViewModel"

@HiltViewModel
class ConfigurationViewModel @Inject constructor(
    private val smbConfigurationRepository: SMBConfigurationRepository,
) : ViewModel() {
    private val _state = MutableLiveData<ConfigState>()
    val state: LiveData<ConfigState> = _state

    val activeConfig = smbConfigurationRepository.config

    val hostname = MutableLiveData(activeConfig.address)
    val shareName = MutableLiveData(activeConfig.shareName)
    val username = MutableLiveData(activeConfig.credentials.username)
    val workgroup = MutableLiveData(activeConfig.credentials.domain)
    val password = MutableLiveData(activeConfig.credentials.password)

    init {
        viewModelScope.launch {
            _state.value = ConfigState()
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.value = ConfigState(
                isTestOngoing = true
            )

            when (val testResult = SMB.testShareLogin(
                hostname.value!!,
                shareName.value!!,
                Credentials(
                    username.value!!,
                    password.value!!,
                    workgroup.value!!
                )
            )) {
                is Result.Success<Boolean> -> {
                    _state.value = ConfigState(
                        testResultMessage = "Successfully established connection.",
                        isSuccessfullyTested = true,
                        isTestable = true,
                        isTestCompleted = true
                    )
                }
                is Result.Error -> {
                    _state.value = ConfigState(
                        testResultMessage = "Failed to establish connection.",
                        testResultExplanation = testResult.exception.message!!,
                        isTestable = true,
                        isTestCompleted = true
                    )
                }
                else -> {
                    throw IllegalStateException("Reached state which should be unreachable!")
                }
            }
        }
    }

    fun invalidateTestResult() {
        _state.value = ConfigState(
            isTestable = isTestable()
        )
    }

    fun storeConfig() {
        Log.i(TAG, "Storing new config hostname of '" + hostname.value + "'")
        smbConfigurationRepository.config = SMBConfiguration(
            hostname.value!!,
            shareName.value!!,
            Credentials(
                username.value!!,
                password.value!!,
                workgroup.value!!
            )
        )
    }

    private fun isTestable(): Boolean {
        return hostname.value!!.isNotBlank() &&
                workgroup.value!!.isNotBlank() &&
                username.value!!.isNotBlank() &&
                shareName.value!!.isNotBlank()
    }


}