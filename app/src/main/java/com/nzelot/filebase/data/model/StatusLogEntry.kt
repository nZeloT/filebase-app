package com.nzelot.filebase.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "status_log")
data class StatusLogEntry(
    @PrimaryKey(autoGenerate = true) val id : Long = 0,
    @ColumnInfo(name = "tmstp") val timestamp : Long,
    @ColumnInfo(name = "severity") val severity: StatusLogSeverity,
    @ColumnInfo(name = "message") val message : String,
    @ColumnInfo(name = "ex_message") val exMessages : String? = null
)