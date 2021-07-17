package com.nzelot.filebase.data.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SMBStateDataStore {

    @Singleton
    @Provides
    fun provideSMBStateDataStore(
        @ApplicationContext context: Context
    ) : DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.dataStoreFile("fileBaseAppState.preferences_pb") }
        )

}