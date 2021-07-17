package com.nzelot.filebase.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject


class SMBStateRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val _pref: Flow<Preferences> = dataStore.data

    val lastSyncDate =
        _pref.map { ZonedDateTime.ofInstant(Instant.ofEpochSecond(it[LAST_SYNC_DATE] ?: 0), ZoneId.systemDefault()) }

    val isSyncOngoing = _pref.map { it[SYNC_ONGOING] ?: false }

    val isShareAvailable = _pref.map { it[SHARE_AVAILABLE] ?: false }

    fun updateIsSyncOngoing(ongoing: Boolean) {
        runBlocking {
            dataStore.edit {
                it[SYNC_ONGOING] = ongoing
            }
        }
    }

    fun updateLastSyncDate(zdt: ZonedDateTime = ZonedDateTime.now()) {
        runBlocking {
            dataStore.edit {
                it[LAST_SYNC_DATE] = zdt.toEpochSecond()
            }
        }
    }

    fun updateShareAvailable(newState : Boolean) {
        runBlocking {
            dataStore.edit {
                it[SHARE_AVAILABLE] = newState
            }
        }
    }

    companion object {
        private val LAST_SYNC_DATE = longPreferencesKey("LastSyncDate")
        private val SYNC_ONGOING = booleanPreferencesKey("SyncOngoing")
        private val SHARE_AVAILABLE = booleanPreferencesKey("ShareAvailable")
    }

}