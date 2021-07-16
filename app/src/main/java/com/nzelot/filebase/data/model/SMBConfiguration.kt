package com.nzelot.filebase.data.model

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class SMBConfiguration(
    val address: String = "",
    val shareName: String = "",
    val credentials: Credentials = Credentials(),
    val syncStartDate: ZonedDateTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())
) {
    fun isComplete(): Boolean =
        address.isNotBlank() && shareName.isNotBlank() && credentials.isComplete() && syncStartDate.isAfter(
            ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault())
        )
}