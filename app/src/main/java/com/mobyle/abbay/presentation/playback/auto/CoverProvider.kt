package com.mobyle.abbay.presentation.playback.auto

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import java.io.File
import java.io.FileNotFoundException

/**
 * Serves book covers to the car host. Covers are SAF documents or app files the
 * host cannot open, so they are exposed as `content://<package>.covers/<encoded uri>`. The provider is not exported:
 * the session grants read access to each URI it hands to a browser.
 */
class CoverProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val original = decode(uri) ?: throw FileNotFoundException(uri.toString())
        val resolver = context?.contentResolver ?: throw FileNotFoundException(uri.toString())
        return when (original.scheme) {
            "file" -> ParcelFileDescriptor.open(File(requireNotNull(original.path)), ParcelFileDescriptor.MODE_READ_ONLY)
            else -> resolver.openFileDescriptor(original, "r")
        } ?: throw FileNotFoundException(uri.toString())
    }

    override fun getType(uri: Uri): String = "image/*"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

        fun authority(context: Context): String = "${context.packageName}.covers"

        /** The provider URI for [coverUri], readable by [packageName] once granted. */
        fun uriFor(context: Context, coverUri: String, packageName: String): Uri {
            val uri = Uri.Builder().scheme("content").authority(authority(context))
                .appendPath(Base64.encodeToString(coverUri.toByteArray(), FLAGS))
                .build()
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            return uri
        }

        private fun decode(uri: Uri): Uri? = uri.lastPathSegment
            ?.let { runCatching { String(Base64.decode(it, FLAGS)) }.getOrNull() }
            ?.let(Uri::parse)
    }
}
