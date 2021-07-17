package com.nzelot.filebase.data.repository

import android.util.Log
import com.nzelot.filebase.data.model.StatusLogEntry
import com.nzelot.filebase.data.model.StatusLogSeverity
import java.time.ZonedDateTime
import javax.inject.Inject

class StatusLogRepository @Inject constructor(
    statusDb : StatusLogDatabase
) {

    private val statusLogDao = statusDb.statusLogDao()

    val logs = statusLogDao.getStatusLogs()

    fun error(tag: String, msg : String, ex : Throwable? = null) {
        Log.e(tag, msg, ex)
        statusLogDao.insertAll(StatusLogEntry(0, ZonedDateTime.now().toEpochSecond(), StatusLogSeverity.ERROR, msg, ex?.message))
    }

    fun info(tag: String, msg : String) {
        Log.i(tag, msg)
        statusLogDao.insertAll(StatusLogEntry(0, ZonedDateTime.now().toEpochSecond(), StatusLogSeverity.INFO, msg))
    }

}