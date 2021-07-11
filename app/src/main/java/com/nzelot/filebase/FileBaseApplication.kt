package com.nzelot.filebase

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.nzelot.filebase.worker.PeriodicFileCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@HiltAndroidApp
class FileBaseApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory : HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        //Create the upload worker
        val work = PeriodicWorkRequestBuilder<PeriodicFileCheckWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresDeviceIdle(true)
                    .build()
            )
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("UploadFileChecker", ExistingPeriodicWorkPolicy.KEEP, work)
    }

    override fun getWorkManagerConfiguration() : Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

//TODO: initially enter config page on application first start
//TODO: Store Last Update time
//TODO: Enable sync now functionality
//TODO: Enable auto update of connected state
//TODO: Stop and restart update worker on config change
//TODO: Update Last sync time in UI after change from worker
//TODO: Enable Status Log by providing Log Collector