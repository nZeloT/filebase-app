package com.nzelot.filebase.data.model

import android.net.Uri
import java.time.LocalDateTime

data class MediaFile(
    val uri: Uri,
    val id: Long,
    val name: String,
    val changedOn: LocalDateTime,
    val fileType: MediaFileType,
)
