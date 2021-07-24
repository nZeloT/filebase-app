package com.nzelot.filebase.worker

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nzelot.filebase.SMB
import com.nzelot.filebase.data.model.MediaFile
import com.nzelot.filebase.data.model.MediaFileType
import com.nzelot.filebase.data.repository.SMBConfigurationRepository
import com.nzelot.filebase.data.repository.SMBStateRepository
import com.nzelot.filebase.data.repository.StatusLogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "org.nzelot.filebase.PeriodicFileCheckWorker"

@HiltWorker
class FileCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context, @Assisted workerParameters: WorkerParameters,
    private val smbConfigurationRepository: SMBConfigurationRepository,
    private val smbStateRepository: SMBStateRepository,
    private val log: StatusLogRepository
) : Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        log.info(
            TAG,
            "Start FileChecker at ${
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }"
        )
        if (isSMBShareAvailable()) {
            log.info(TAG, "Share is available.")

            if (getIsSyncOngoing()) {
                log.info(TAG, "There is a parallel sync ongoing; Stopping this one;")
                return Result.success()
            } else {
                setSyncOngoing(true)
            }

            val lastSync = getLastSyncDate()
            val newMedia = mutableListOf<MediaFile>()
            newMedia += getNewImagesSince(lastSync)
            newMedia += getNewVideosSince(lastSync)

            val newMediaUris = newMedia.map { it.uri.toString() }.toTypedArray()
            val newMediaName = newMedia.map { it.name }.toTypedArray()

            val zdt = ZonedDateTime.now(ZoneId.systemDefault())
            log.info(TAG, "Found ${newMedia.size} new Media Files.")
            log.info(
                TAG, "Last Sync Time is now ${
                    zdt.format(
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    )
                }"
            )

            smbStateRepository.updateLastSyncDate(zdt)

            if (newMedia.isNotEmpty()) {
                val inputData = Data.Builder()
                    .putStringArray(INPUT_URI_ARR, newMediaUris)
                    .putStringArray(INPUT_NAME_VAL, newMediaName)
                    .putInt(INPUT_UPLOAD_ATTEMPT, 0)
                    .build()

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    .build()

                val worker = OneTimeWorkRequestBuilder<SMBTransferWorker>()
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .addTag(SMBTransferWorker.UPLOAD_TAG)
                    .build()

                log.info(TAG, "Upload requested for ${newMediaUris.size} files.")
                WorkManager.getInstance(applicationContext).enqueue(worker)
            }

            setSyncOngoing(false)
        } else {
            log.error(TAG, "Share is not available; Quitting;")
        }

        log.info(
            TAG,
            "Finished FileChecker at ${
                ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }"
        )

        return Result.success()
    }

    private fun isSMBShareAvailable(): Boolean {
        val config = smbConfigurationRepository.config
        return when (SMB.testShareLoginBlocking(config)) {
            is com.nzelot.filebase.data.Result.Success<Boolean> -> true
            is com.nzelot.filebase.data.Result.Error -> false
        }
    }

    private fun getLastSyncDate(): ZonedDateTime {
        return runBlocking {
            smbStateRepository.lastSyncDate.first()
        }
    }

    private fun getIsSyncOngoing(): Boolean {
        return runBlocking {
            smbStateRepository.isSyncOngoing.first()
        }
    }

    private fun setSyncOngoing(isOngoing: Boolean) {
        smbStateRepository.updateIsSyncOngoing(isOngoing)
    }

    private fun getNewImagesSince(dt: ZonedDateTime): List<MediaFile> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
        )

        val selection = "${MediaStore.Images.Media.DATE_MODIFIED} >= ?"
        val selectionArgs = arrayOf(
            dt.toEpochSecond().toString()
        )

        val query = applicationContext.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )

        return queryMedia(
            query,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaFileType.IMAGE
        )
    }

    private fun getNewVideosSince(dt: ZonedDateTime): List<MediaFile> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
        )
        val selection = "${MediaStore.Video.Media.DATE_MODIFIED} >= ?"
        val selectionArgs = arrayOf(
            dt.toEpochSecond().toString()
        )
        val query = applicationContext.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )
        return queryMedia(
            query,
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaFileType.VIDEO
        )
    }

    private fun queryMedia(
        query: Cursor?,
        idCol: String,
        nameCol: String,
        dateModCol: String,
        fileType: MediaFileType
    ): List<MediaFile> {
        val mediaList = mutableListOf<MediaFile>()

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(idCol)
            val nameColumn = cursor.getColumnIndexOrThrow(nameCol)
            val dateModColumn = cursor.getColumnIndexOrThrow(dateModCol)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateMod = LocalDateTime.ofEpochSecond(
                    cursor.getInt(dateModColumn).toLong(), 0, ZoneOffset.UTC
                )

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val file = MediaFile(contentUri, id, name, dateMod, fileType)
                mediaList += file
                Log.d(TAG, "Found new Media File $file")
            }
        }

        return mediaList
    }

    companion object {
        const val CHECKER_TAG = "FilebaseFileChecker"
    }
}