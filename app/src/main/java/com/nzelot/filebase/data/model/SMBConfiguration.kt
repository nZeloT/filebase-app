package com.nzelot.filebase.data.model

data class SMBConfiguration(
    val address: String = "",
    val shareName: String = "",
    val credentials: Credentials = Credentials(),
) {
    fun isComplete() : Boolean = address.isNotBlank() && shareName.isNotBlank() && credentials.isComplete()
}