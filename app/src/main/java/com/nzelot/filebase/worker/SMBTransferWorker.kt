package com.nzelot.filebase.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.io.ArrayByteChunkProvider
import com.nzelot.filebase.BuildConfig
import com.nzelot.filebase.SMB
import com.nzelot.filebase.data.model.SMBConfiguration
import com.nzelot.filebase.data.repository.SMBConfigurationRepository
import com.nzelot.filebase.data.repository.StatusLogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "org.nzelot.filebase.SMBTransferWorker"
private const val FILE_BASE_ROOT = ".filebase"

const val INPUT_URI_ARR = "INPUT_URIS"
const val INPUT_MIME_VAL = "INPUT_MIME"
const val INPUT_UPLOAD_ATTEMPT = "INPUT_UPLOAD_ATTEMPT"

@HiltWorker
class SMBTransferWorker @AssistedInject constructor(
    @Assisted appContext: Context, @Assisted workerParameters: WorkerParameters,
    private val smbConfigurationRepository: SMBConfigurationRepository,
    private val log : StatusLogRepository
) : Worker(appContext, workerParameters){

    override fun doWork(): Result {
        log.info(TAG, "Starting Upload ...")
        val urisToUpload = inputData.getStringArray(INPUT_URI_ARR)!!
        val mimeToUpload = inputData.getStringArray(INPUT_MIME_VAL)!!
        val uploadAttempt = inputData.getInt(INPUT_UPLOAD_ATTEMPT, 0)

        if (BuildConfig.DEBUG && urisToUpload.size != mimeToUpload.size) {
            log.error(TAG, "Mismatch between Uris and MimeType!!")
            error("Mismatch between Uris and MimeType!")
        }


        log.info(TAG, "Received ${urisToUpload.size} new Files to upload to smb share; Queued for attempt $uploadAttempt")

        var lastSuccessfulIdx = -1

        val smbShareConfig = getShareConfiguration()
        val uploadState = SMB.doOnShare(smbShareConfig) { diskShare ->
            log.info(TAG, "Connected to share. Starting upload ...")

            //check that destination directory exists
            if(!diskShare.folderExists(FILE_BASE_ROOT)){
                diskShare.mkdir(FILE_BASE_ROOT)
            }

            for (item in urisToUpload.indices) {

                //1. read local file
                val localFile = when (val r = readLocalMediaFile(urisToUpload[item])) {
                    is com.nzelot.filebase.data.Result.Success<ByteArray> -> {
                        r.data
                    }
                    is com.nzelot.filebase.data.Result.Error -> {
                        log.error(TAG, "Received a read error on local file; will skip;", r.exception)
                        ++lastSuccessfulIdx
                        continue
                    }
                }

                //1.5 derive file ending from mime-type
                //    expecting mime types like 'image/jpeg' or 'image/png' or 'video/mp4'
                val mimeType = mimeToUpload[item]
                Log.d(TAG, "Received Mime-Type of $mimeType")

                val fileCategory = mimeType.split('/')[0]
                val fileEnding = ".${mimeType.split('/')[1]}"
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
                val fileName = "${fileCategory}_${timestamp}.$fileEnding"
                val smbFilePath = "$FILE_BASE_ROOT\\$fileName"

                log.info(TAG, "Uploading file to $smbFilePath")

                //2. create remote file
                val smbFile = diskShare.openFile(
                    smbFilePath,
                    EnumSet.of(AccessMask.GENERIC_WRITE),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    null
                )

                //3. upload file
                smbFile.write(ArrayByteChunkProvider(localFile, 0))
                smbFile.close()

                ++lastSuccessfulIdx
            }

            com.nzelot.filebase.data.Result.Success(true)
        }

        when (uploadState) {
            is com.nzelot.filebase.data.Result.Success<Boolean> -> {
                log.info(TAG, "Successfully finished Uploading")
            }
            is com.nzelot.filebase.data.Result.Error -> {
                log.error(TAG, "An error occurred during upload!", uploadState.exception)

                //we missed some files; reschedule the missed ones
                val missedUris =
                    urisToUpload.slice(IntRange(lastSuccessfulIdx + 1, urisToUpload.size - 1)).toTypedArray()
                val missedMime =
                    mimeToUpload.slice(IntRange(lastSuccessfulIdx + 1, mimeToUpload.size - 1)).toTypedArray()
                val nextUploadAttempt = uploadAttempt + 1

                val inputData = Data.Builder()
                    .putStringArray(INPUT_URI_ARR, missedUris)
                    .putStringArray(INPUT_MIME_VAL, missedMime)
                    .putInt(INPUT_UPLOAD_ATTEMPT, nextUploadAttempt)
                    .build()

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(true)
                    .setRequiresDeviceIdle(true)
                    .build()

                val work = OneTimeWorkRequestBuilder<SMBTransferWorker>()
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .setInitialDelay(1, TimeUnit.HOURS)
                    .build()

                log.info(TAG, "Missed to upload ${missedUris.size} of initially ${urisToUpload.size}; Requeueing with for attempt $nextUploadAttempt")
                WorkManager.getInstance(applicationContext).enqueue(work)
            }
        }

        log.info(TAG, "Closing upload worker")
        return Result.success()
    }

    private fun readLocalMediaFile(uri: String) : com.nzelot.filebase.data.Result<ByteArray> {
        var inputStream : InputStream? = null
        return try {
            inputStream = applicationContext.contentResolver.openInputStream(Uri.parse(uri))
            if(inputStream != null) {
                com.nzelot.filebase.data.Result.Success(inputStream.readBytes())
            }else{
                com.nzelot.filebase.data.Result.Error(NullPointerException("Received InputStream == null!"))
            }
        }catch(ex : IOException) {
            com.nzelot.filebase.data.Result.Error(ex)
        }finally {
            inputStream?.close()
        }
    }

    private fun getShareConfiguration() : SMBConfiguration {
        return smbConfigurationRepository.config
    }


}