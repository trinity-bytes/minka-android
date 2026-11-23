package com.trinitybytes.minka.features.items.presentation.publish

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PHOTOS_DIR = "listing_photos"
private const val MAX_PHOTO_BYTES = 25L * 1024 * 1024

/**
 * Where the photos of a draft live. A picked photo is only readable while its picker grant lasts, so
 * it is copied into app-private storage; the draft keeps the `file:` URI of the copy and survives
 * restarts. The camera writes straight into the same folder through a [FileProvider] URI, which
 * file_paths.xml limits to this folder.
 */
internal object PublishPhotoFiles {

    /** A new empty file for the camera, and the content URI to hand to it. */
    fun newCameraTarget(context: Context): Pair<File, Uri> {
        val file = newFile(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    /** Copies the picked [source] into the photos folder; null when it cannot be read or is not an image. */
    suspend fun import(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        if (resolver.getType(source)?.startsWith("image/") != true) return@withContext null
        val target = newFile(context)
        try {
            resolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            if (target.length() == 0L || target.length() > MAX_PHOTO_BYTES) {
                target.delete()
                null
            } else {
                target.toUri().toString()
            }
        } catch (_: IOException) {
            target.delete()
            null
        } catch (_: SecurityException) {
            target.delete()
            null
        }
    }

    /** The URI the draft keeps for a camera [file]; null when the camera left nothing behind. */
    fun cameraResult(file: File): String? = file.takeIf { it.length() > 0L }?.toUri()?.toString()

    fun discard(file: File) {
        file.delete()
    }

    private fun newFile(context: Context): File {
        val directory = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
        return File(directory, "${UUID.randomUUID()}.jpg")
    }
}
