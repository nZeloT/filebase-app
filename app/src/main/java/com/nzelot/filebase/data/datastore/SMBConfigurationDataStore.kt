package com.nzelot.filebase.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.nzelot.filebase.crypto.Crypto
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SMBConfigurationDataStore {

    @Provides
    fun providesSerializer(crypto: Crypto) : Serializer<SMBConfigurationStored>
        = SMBConfigurationStoredSerializer(crypto)

    @Provides
    fun providesDataStore(
        @ApplicationContext context: Context,
        serializer: SMBConfigurationStoredSerializer
    ) : DataStore<SMBConfigurationStored> =
        DataStoreFactory.create(
            serializer = serializer,
            produceFile = { context.dataStoreFile("smbShareConfiguration.pb") }
        )

}