package com.nzelot.filebase.crypto

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.KeyStore
import javax.inject.Named

@Module(includes = [SecurityModule.Declarations::class])
@InstallIn(SingletonComponent::class)
object SecurityModule {
    const val KEY_NAME = "FILEBASE"
    const val KEY_STORE_NAME = "FILEBASE_STORE"

    private const val ANDROID_KEY_STORE_TYPE = "AndroidKeyStore"
    private const val FILEBASE_DATA_KEY_NAME = "FileBaseDataKey"

    @Provides
    fun provideKeyStore() : KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE_TYPE).apply { load(null) }

    @Provides
    @Named(KEY_NAME)
    fun providesKeyName() : String = FILEBASE_DATA_KEY_NAME

    @Provides
    @Named(KEY_STORE_NAME)
    fun providesKeyStoreName() : String = ANDROID_KEY_STORE_TYPE

    @Module
    @InstallIn(SingletonComponent::class)
    interface Declarations {
        @Binds
        fun bindsCipherProvider(impl : AesCipherProvider) : CipherProvider

        @Binds
        fun bindCrypto(impl : CryptoImpl) : Crypto
    }

}