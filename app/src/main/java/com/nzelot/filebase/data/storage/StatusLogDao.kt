package com.nzelot.filebase.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nzelot.filebase.data.model.StatusLogEntry
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.ZonedDateTime

@Dao
interface StatusLogDao {

    @Query("SELECT * from status_log WHERE tmstp > :startingFrom")
    fun getStatusLogs(startingFrom : Long = (ZonedDateTime.now() - Duration.ofHours(24)).toEpochSecond()) : Flow<List<StatusLogEntry>>

    @Insert
    fun insertAll(vararg entries : StatusLogEntry)

}