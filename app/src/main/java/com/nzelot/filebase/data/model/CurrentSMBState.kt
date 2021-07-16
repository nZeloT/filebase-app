package com.nzelot.filebase.data.model

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class CurrentSMBState(
    val isSyncOngoing : Boolean = false,
    val isShareAvailable : Boolean = false,
    val lastSyncZdt : ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.systemDefault())
)
