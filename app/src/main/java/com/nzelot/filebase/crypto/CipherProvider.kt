package com.nzelot.filebase.crypto

import javax.crypto.Cipher

interface CipherProvider {
    val encryptCipher : Cipher
    fun decryptCipher(iv : ByteArray) : Cipher
}