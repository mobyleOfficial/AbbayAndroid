package com.mobyle.abbay.data.backup

import android.content.Context
import android.net.Uri
import com.model.DataError
import com.model.DataResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Reads and writes the backup document the user picked (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`). */
class BackupFileSource @Inject constructor(@ApplicationContext private val context: Context) {
    fun write(documentUri: String, text: String): DataResult<Unit> = guarded {
        context.contentResolver.openOutputStream(Uri.parse(documentUri), "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            ?: error("Cannot write the backup")
    }

    fun read(documentUri: String): DataResult<String> = guarded {
        context.contentResolver.openInputStream(Uri.parse(documentUri))?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Cannot read the backup")
    }

    /** Data sources map every failure to a [DataResult]. */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private inline fun <T> guarded(block: () -> T): DataResult<T> = try {
        DataResult.Success(block())
    } catch (securityException: SecurityException) {
        DataResult.Failure(DataError.ACCESS_LOST)
    } catch (exception: Exception) {
        DataResult.Failure(DataError.UNREADABLE)
    }
}
