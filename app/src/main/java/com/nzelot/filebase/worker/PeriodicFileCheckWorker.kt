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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val TAG = "org.nzelot.filebase.PeriodicFileCheckWorker"

@HiltWorker
class PeriodicFileCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context, @Assisted workerParameters: WorkerParameters,
    private val smbConfigurationRepository : SMBConfigurationRepository
) : Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        if (isSMBShareAvailable()) {
            val lastSync = getLastSyncDate()

            val newMedia = mutableListOf<MediaFile>()
            newMedia += getNewImagesSince(lastSync)
            newMedia += getNewVideosSince(lastSync)

            val newMediaUris = newMedia.map { it.uri.toString() }.toTypedArray()
            val newMediaMime = newMedia.map { it.mimeType }.toTypedArray()

            Log.i(TAG, "Scheduling ${newMedia.size} new Files for Upload. Last Sync Time is now ${LocalDateTime.now()}")
            //TODO write LocalDateTime.now() back into storage

            if (newMedia.isNotEmpty()) {
                val inputData = Data.Builder()
                    .putStringArray(INPUT_URI_ARR, newMediaUris)
                    .putStringArray(INPUT_MIME_VAL, newMediaMime)
                    .putInt(INPUT_UPLOAD_ATTEMPT, 0)
                    .build()
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    .setRequiresDeviceIdle(true)
                    .build()
                val worker = OneTimeWorkRequestBuilder<SMBTransferWorker>()
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(applicationContext).enqueue(worker)
            }
        }

        return Result.success()
    }

    private fun isSMBShareAvailable(): Boolean {
        val config = smbConfigurationRepository.config
        return when (SMB.testShareLoginBlocking(config)) {
            is com.nzelot.filebase.data.Result.Success<Boolean> -> true
            is com.nzelot.filebase.data.Result.Error -> false
        }
    }

    private fun getLastSyncDate(): LocalDateTime {
        //TODO read last sync time from storage
        return LocalDateTime.of(2021, 6, 1, 0, 0)
    }

    private fun getNewImagesSince(dt: LocalDateTime): List<MediaFile> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Images.Media.DATE_MODIFIED} >= ?"
        val selectionArgs = arrayOf(
            dt.toEpochSecond(ZoneOffset.UTC).toString()
        )

        val query = applicationContext.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )

        return queryMedia(query, MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.MIME_TYPE, MediaFileType.IMAGE)
    }

    private fun getNewVideosSince(dt: LocalDateTime): List<MediaFile> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        val selection = "${MediaStore.Video.Media.DATE_MODIFIED} >= ?"
        val selectionArgs = arrayOf(
            dt.toEpochSecond(ZoneOffset.UTC).toString()
        )
        val query = applicationContext.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            null
        )
        return queryMedia(query, MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATE_MODIFIED, MediaStore.Video.Media.MIME_TYPE, MediaFileType.VIDEO)
    }

    private fun queryMedia(query: Cursor?, idCol : String, nameCol : String, dateModCol : String, mimeCol: String, fileType : MediaFileType) : List<MediaFile> {
        val mediaList = mutableListOf<MediaFile>()

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(idCol)
            val nameColumn = cursor.getColumnIndexOrThrow(nameCol)
            val dateModColumn = cursor.getColumnIndexOrThrow(dateModCol)
            val mimColumn = cursor.getColumnIndexOrThrow(mimeCol)

            while(cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mimType = cursor.getString(mimColumn)
                val dateMod = LocalDateTime.ofEpochSecond(
                    cursor.getInt(dateModColumn).toLong(), 0, ZoneOffset.UTC)

                val contentUri : Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val file = MediaFile(contentUri, id, name, dateMod, fileType, mimType)
                mediaList += file
                Log.d(TAG, "Found new Media File $file")
            }
        }

        return mediaList
    }
}