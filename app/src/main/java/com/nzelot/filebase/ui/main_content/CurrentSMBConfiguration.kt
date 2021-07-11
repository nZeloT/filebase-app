package com.nzelot.filebase.ui.main_content

import java.time.LocalDateTime

data class CurrentSMBConfiguration(
    val address: String,
    val username: String,
    val lastSync: LocalDateTime
)