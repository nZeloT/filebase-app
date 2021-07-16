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
import com.nzelot.filebase.data.repository.SMBStateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

private const val TAG = "org.nzelot.filebase.ServerConfigViewModel"

@HiltViewModel
class ConfigurationViewModel @Inject constructor(
    private val smbConfigurationRepository: SMBConfigurationRepository,
    private val smbStateRepository: SMBStateRepository
) : ViewModel() {
    private val _state = MutableLiveData<ConfigState>()
    val state: LiveData<ConfigState> = _state

    val hostname = MutableLiveData<String>()
    val shareName = MutableLiveData<String>()
    val username = MutableLiveData<String>()
    val workgroup = MutableLiveData<String>()
    val password = MutableLiveData<CharArray>()

    private val _syncStart = MutableLiveData<ZonedDateTime>()
    val syncStart : LiveData<ZonedDateTime> = _syncStart

    init {
        viewModelScope.launch {
            Log.d(TAG, "Init View Model")
            _state.value = ConfigState()

            val activeConfig = smbConfigurationRepository.config
            hostname.value = activeConfig.address
            shareName.value = activeConfig.shareName
            username.value = activeConfig.credentials.username
            workgroup.value = activeConfig.credentials.domain
            password.value = activeConfig.credentials.password
            _syncStart.value = activeConfig.syncStartDate

            Log.d(TAG, "Initialized with config $activeConfig")
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
                        isTestCompleted = true,
                        isStoreable = isStoreable(true)
                    )
                }
                is Result.Error -> {
                    _state.value = ConfigState(
                        testResultMessage = "Failed to establish connection.",
                        testResultExplanation = testResult.exception.message!!,
                        isTestable = true,
                        isTestCompleted = true,
                        isStoreable = isStoreable(false)
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

    fun updateSyncDate(year : Int, month : Int, day : Int) {
        viewModelScope.launch {
            Log.d(TAG, "Setting new Date Value $year - $month - $day")
            _syncStart.value = ZonedDateTime.of(year, month, day, 0, 0, 0, 0, ZoneId.systemDefault())
            val s = _state.value!!
            _state.value = ConfigState(
                isTestable = isTestable(),
                isSuccessfullyTested = s.isSuccessfullyTested,
                isTestOngoing = s.isTestOngoing,
                isTestCompleted = s.isTestCompleted,
                testResultMessage = s.testResultMessage,
                testResultExplanation = s.testResultExplanation,
                isStoreable = isStoreable(s.isSuccessfullyTested)
            )
        }
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
            ),
            syncStart.value!!
        )
        smbStateRepository.updateLastSyncDate(syncStart.value!!)
    }

    private fun isTestable(): Boolean {
        return hostname.value!!.isNotBlank() &&
                workgroup.value!!.isNotBlank() &&
                username.value!!.isNotBlank() &&
                shareName.value!!.isNotBlank()
    }

    fun isStoreable(testedSuccessfully : Boolean = state.value!!.isSuccessfullyTested) : Boolean {
        return  testedSuccessfully && isDateAfterEpoch(syncStart.value!!)
    }

    private fun isDateAfterEpoch(zdt : ZonedDateTime) : Boolean {
        return zdt.isAfter(
            ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())
        )
    }
}