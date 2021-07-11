package com.nzelot.filebase.crypto

import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

interface Crypto {
    fun encrypt(rawBytes : ByteArray, outputStream: OutputStream)
    fun decrypt(intpuStream: InputStream) : ByteArray
}


class CryptoImpl @Inject constructor(
    private val cipherProvider: CipherProvider
): Crypto {

    override fun encrypt(rawBytes: ByteArray, outputStream: OutputStream) {
        val cipher = cipherProvider.encryptCipher
        val encryptedBytes = cipher.doFinal(rawBytes)
        with(outputStream) {
            write(cipher.iv.size)
            write(cipher.iv)
            write(encryptedBytes)
        }
    }

    override fun decrypt(intpuStream: InputStream): ByteArray {
        val ivSize = intpuStream.read()
        val iv = ByteArray(ivSize)
        intpuStream.read(iv)
        val encrypted = intpuStream.readBytes()
        val cipher = cipherProvider.decryptCipher(iv)
        return cipher.doFinal(encrypted)
    }
}