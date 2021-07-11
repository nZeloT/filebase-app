package com.nzelot.filebase.data.repository

import androidx.datastore.core.DataStore
import com.nzelot.filebase.data.datastore.SMBConfigurationStored
import com.nzelot.filebase.data.model.Credentials
import com.nzelot.filebase.data.model.SMBConfiguration
import com.nzelot.filebase.ui.main_content.CurrentSMBConfiguration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject

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
                LocalDateTime.now() //TODO read from application state datastore
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
                    .build()
            }
        }
    }

    private fun readStoredConfiguration() : SMBConfiguration {
        return try {
            runBlocking {
                val c = dataStore.data.first()
                SMBConfiguration(c.address, c.share, Credentials(c.username, c.password.toCharArray(), c.workgroup))
            }
        }catch (ex : NoSuchElementException){
            SMBConfiguration()
        }
    }

}