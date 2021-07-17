package com.nzelot.filebase.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import com.nzelot.filebase.data.storage.SMBConfigurationStored
import com.nzelot.filebase.data.model.Credentials
import com.nzelot.filebase.data.model.SMBConfiguration
import com.nzelot.filebase.data.model.CurrentSMBConfiguration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "org.nzelot.filebase.SMBConfigurationRepository"

class SMBConfigurationRepository @Inject constructor(
    private val dataStore: DataStore<SMBConfigurationStored>
) {

    var config : SMBConfiguration
        get() = readStoredConfiguration()
        set(value) = storeConfiguration(value)

    val currentConfiguration : CurrentSMBConfiguration
        get() {
            val c = config
            return CurrentSMBConfiguration(
                "\\\\${c.address}\\${c.shareName}",
                "${c.credentials.domain}\\\\${c.credentials.username}",
            )
        }

    private fun storeConfiguration(conf: SMBConfiguration) {
        runBlocking {
            dataStore.updateData {
                it.toBuilder()
                    .setAddress(conf.address)
                    .setWorkgroup(conf.credentials.domain)
                    .setUsername(conf.credentials.username)
                    .setPassword(conf.credentials.password.toString())
                    .setShare(conf.shareName)
                    .setStartSyncDate(conf.syncStartDate.format(DateTimeFormatter.ISO_INSTANT))
                    .build()
            }
        }
    }

    private fun readStoredConfiguration() : SMBConfiguration {
        return try {
            runBlocking {
                Log.d(TAG, "Reading new configuration from the repository")
                val c = dataStore.data.first()
                SMBConfiguration(c.address, c.share, Credentials(c.username, c.password.toCharArray(), c.workgroup))
            }
        }catch (ex : NoSuchElementException){
            SMBConfiguration()
        }
    }

}