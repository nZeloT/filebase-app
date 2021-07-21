package com.nzelot.filebase.data.storage

import android.util.Log
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.nzelot.filebase.crypto.Crypto
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

const val TAG = "com.nzelot.filebase.SMBConfigurationSerializer"

class SMBConfigurationStoredSerializer @Inject constructor(
    private val crypto: Crypto
) : Serializer<SMBConfigurationStored> {

    override val defaultValue: SMBConfigurationStored = SMBConfigurationStored.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SMBConfigurationStored {
        return try {
            val decrypted = crypto.decrypt(input)
            SMBConfigurationStored.parseFrom(decrypted)
        } catch (ex: InvalidProtocolBufferException) {
            throw CorruptionException("can't read proto!", ex)
        }
    }

    override suspend fun writeTo(t: SMBConfigurationStored, output: OutputStream) {
        Log.d(TAG, "Writing crypted SMB data.")
        val dataToStore = t.toByteArray()
        crypto.encrypt(dataToStore, output)
    }
}