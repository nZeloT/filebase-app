package com.nzelot.filebase.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.nzelot.filebase.data.model.CurrentSMBState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject


class SMBStateRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val _pref: Preferences
        get() = readStoredPreferences()

    val state : CurrentSMBState
        get() = CurrentSMBState(readIsSyncOngoing(), readIsShareAvailable(), readLastSyncDate())

    fun updateIsSyncOngoing(ongoing : Boolean) {
        runBlocking {
            dataStore.edit {
                it[SYNC_ONGOING] = ongoing
            }
        }
    }

    fun updateLastSyncDate(zdt : ZonedDateTime = ZonedDateTime.now()) {
        runBlocking {
            dataStore.edit {
                it[LAST_SYNC_DATE] = zdt.toEpochSecond()
            }
        }
    }

    private fun readLastSyncDate() : ZonedDateTime {
        val epochTime = _pref[LAST_SYNC_DATE] ?: 0
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochTime), ZoneId.systemDefault())!!
    }

    private fun readIsSyncOngoing() : Boolean {
        return _pref[SYNC_ONGOING] ?: false
    }

    private fun readIsShareAvailable() : Boolean {
        return _pref[SHARE_AVAILABLE] ?: false
    }

    private fun readStoredPreferences(): Preferences {
        return runBlocking {
            dataStore.data.first()
        }
    }

    companion object {
        private val LAST_SYNC_DATE = longPreferencesKey("LastSyncDate")
        private val SYNC_ONGOING = booleanPreferencesKey("SyncOngoing")
        private val SHARE_AVAILABLE = booleanPreferencesKey("ShareAvailable")
    }

}