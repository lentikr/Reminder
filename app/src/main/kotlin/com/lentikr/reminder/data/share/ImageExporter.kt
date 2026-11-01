package com.lentikr.reminder.data.share

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Writes rendered reminder images to the gallery or to a shareable cache file. */
@Singleton
class ImageExporter @Inject constructor(@ApplicationContext private val context: Context) {

  /** Saves [bitmap] to Pictures/Reminder. MediaStore needs no permission on Android 10+. */
  suspend fun saveToGallery(bitmap: Bitmap, displayName: String): Uri =
    withContext(Dispatchers.IO) {
      val resolver = context.contentResolver
      val values =
        ContentValues().apply {
          put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
          put(MediaStore.Images.Media.MIME_TYPE, "image/png")
          put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Reminder")
          put(MediaStore.Images.Media.IS_PENDING, 1)
        }
      val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert failed")
      try {
        val stream = resolver.openOutputStream(uri) ?: throw IOException("Cannot open $uri")
        stream.use { bitmap.compressPng(it) }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        uri
      } catch (e: Exception) {
        resolver.delete(uri, null, null)
        throw e
      }
    }

  /** Writes [bitmap] to cache/shared/ and returns a content URI for the share sheet. */
  suspend fun prepareShare(bitmap: Bitmap): Uri =
    withContext(Dispatchers.IO) {
      val dir = File(context.cacheDir, "shared").apply { mkdirs() }
      dir.listFiles()?.forEach { it.delete() }
      val file = File(dir, "reminder-${System.currentTimeMillis()}.png")
      file.outputStream().use { bitmap.compressPng(it) }
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

  private fun Bitmap.compressPng(stream: java.io.OutputStream) {
    val software = if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this
    try {
      if (!software.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw IOException("PNG encoding failed")
    } finally {
      if (software !== this) software.recycle()
    }
  }
}
