package com.weekflow.app.core.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.LruCache
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.weekflow.app.domain.model.TaskAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

object TaskAttachmentHelper {

    private const val ATTACHMENTS_DIR = "attachments"
    private const val THUMBNAIL_CACHE_MAX_DIMENSION = 256
    private val IMAGE_MIME_BY_EXTENSION = mapOf(
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp",
        "gif" to "image/gif",
        "bmp" to "image/bmp",
        "heic" to "image/heic",
        "heif" to "image/heif"
    )
    private val EXTENSION_BY_IMAGE_MIME = mapOf(
        "image/jpeg" to "jpg",
        "image/jpg" to "jpg",
        "image/png" to "png",
        "image/webp" to "webp",
        "image/gif" to "gif",
        "image/bmp" to "bmp",
        "image/heic" to "heic",
        "image/heif" to "heif"
    )
    private val IMAGE_EXTENSIONS = IMAGE_MIME_BY_EXTENSION.keys
    private val thumbnailCache = LruCache<String, ImageBitmap>(48)

    internal fun resolveImageMimeType(attachment: TaskAttachment): String {
        val rawMime = attachment.mimeType.trim().lowercase(Locale.US)
        if (rawMime.startsWith("image/") && rawMime != "image/*") {
            return rawMime
        }

        val extFromName = extractLowercaseExtension(attachment.displayName)
        IMAGE_MIME_BY_EXTENSION[extFromName]?.let { return it }

        val extFromPath = extractLowercaseExtension(attachment.localPath)
        IMAGE_MIME_BY_EXTENSION[extFromPath]?.let { return it }

        return "image/*"
    }

    internal fun buildStorageFileName(displayName: String, mimeType: String?): String {
        val sanitized = displayName
            .trim()
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .takeLast(80)
            .ifBlank { "attachment" }

        val lastDot = sanitized.lastIndexOf('.')
        val hasValidExtension = lastDot > 0 && lastDot < sanitized.length - 1
        if (hasValidExtension) {
            val baseName = sanitized.substring(0, lastDot)
            val ext = sanitized.substring(lastDot + 1)
            val lowerExt = ext.lowercase(Locale.US)
            return if (ext == lowerExt) sanitized else "$baseName.$lowerExt"
        }

        val resolvedExt = extensionFromMimeType(mimeType)
        if (resolvedExt.isNullOrBlank()) {
            return sanitized
        }

        val cleanBase = if (lastDot == sanitized.length - 1 && lastDot > 0) {
            sanitized.dropLast(1)
        } else {
            sanitized
        }
        return "$cleanBase.$resolvedExt"
    }

    private fun extractLowercaseExtension(nameOrPath: String): String {
        val fileName = nameOrPath.trim().substringAfterLast('/').substringAfterLast('\\')
        val lastDot = fileName.lastIndexOf('.')
        if (lastDot <= 0 || lastDot >= fileName.length - 1) return ""
        return fileName.substring(lastDot + 1).lowercase(Locale.US)
    }

    private fun extensionFromMimeType(mimeType: String?): String? {
        val normalized = mimeType?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotBlank() } ?: return null
        val fromAndroid = runCatching {
            MimeTypeMap.getSingleton()?.getExtensionFromMimeType(normalized)
        }.getOrNull()?.takeIf { it.isNotBlank() }
        return (fromAndroid ?: EXTENSION_BY_IMAGE_MIME[normalized])?.lowercase(Locale.US)
    }

    fun copyUriToInternalStorage(context: Context, uri: Uri): TaskAttachment? {
        return runCatching {
            val resolver = context.contentResolver
            var originalName: String? = null
            var reportedSize: Long = -1L

            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) {
                            originalName = cursor.getString(nameIdx)
                        }
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                            reportedSize = cursor.getLong(sizeIdx)
                        }
                    }
                }

            val rawMimeType = resolver.getType(uri)
            val fallbackExt = extensionFromMimeType(rawMimeType)
            val displayName = originalName?.takeIf { it.isNotBlank() }
                ?: buildString {
                    append("file_")
                    append(System.currentTimeMillis())
                    if (!fallbackExt.isNullOrBlank()) {
                        append(".")
                        append(fallbackExt)
                    }
                }

            val mimeType = rawMimeType?.takeIf { it.isNotBlank() }
                ?: extractLowercaseExtension(displayName)
                    .takeIf { it.isNotBlank() }
                    ?.let { ext ->
                        IMAGE_MIME_BY_EXTENSION[ext]
                            ?: runCatching { MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(ext) }.getOrNull()
                    }
                ?: "application/octet-stream"

            val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIR)
            if (!attachmentsDir.exists()) {
                attachmentsDir.mkdirs()
            }

            val attachmentId = UUID.randomUUID().toString()
            val storageFileName = buildStorageFileName(displayName, mimeType)
            val destFile = File(attachmentsDir, "${attachmentId}_$storageFileName")

            val inputStream = resolver.openInputStream(uri) ?: return null
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val actualSize = destFile.length().takeIf { it > 0L }
                ?: reportedSize.coerceAtLeast(0L)

            TaskAttachment(
                id = attachmentId,
                displayName = displayName,
                localPath = destFile.absolutePath,
                mimeType = mimeType,
                sizeBytes = actualSize
            )
        }.getOrNull()
    }

    fun openAttachment(context: Context, attachment: TaskAttachment) {
        val file = File(attachment.localPath)
        if (!file.exists()) {
            Toast.makeText(context, "Файл не найден", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, attachment.mimeType.ifBlank { "*/*" })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Нет приложения для открытия файла", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
        }
    }

    fun isImageAttachment(attachment: TaskAttachment): Boolean {
        if (attachment.mimeType.trim().startsWith("image/", ignoreCase = true)) {
            return true
        }
        val nameExt = extractLowercaseExtension(attachment.displayName)
        if (nameExt in IMAGE_EXTENSIONS) {
            return true
        }
        val pathExt = extractLowercaseExtension(attachment.localPath)
        return pathExt in IMAGE_EXTENSIONS
    }

    fun copyImageToClipboard(context: Context, attachment: TaskAttachment): Boolean {
        if (!isImageAttachment(attachment) || attachment.localPath.isBlank()) {
            Toast.makeText(context, "Не удалось скопировать фото", Toast.LENGTH_SHORT).show()
            return false
        }

        val file = File(attachment.localPath)
        if (!file.exists() || !file.isFile) {
            Toast.makeText(context, "Не удалось скопировать фото", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: throw IllegalStateException("ClipboardManager unavailable")
            val clipLabel = attachment.displayName.ifBlank { "Фото" }
            val clipData = ClipData(
                ClipDescription(clipLabel, arrayOf(resolveImageMimeType(attachment))),
                ClipData.Item(contentUri)
            )
            clipboard.setPrimaryClip(clipData)
            Toast.makeText(context, "Фото скопировано", Toast.LENGTH_SHORT).show()
            true
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось скопировать фото", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun getCachedBitmap(localPath: String, maxDimensionPx: Int): ImageBitmap? {
        if (maxDimensionPx > THUMBNAIL_CACHE_MAX_DIMENSION) return null
        return thumbnailCache.get("$localPath@$maxDimensionPx")
    }

    suspend fun decodeSampledBitmap(localPath: String, maxDimensionPx: Int): ImageBitmap? {
        if (maxDimensionPx <= 0 || localPath.isBlank()) return null

        val cacheKey = "$localPath@$maxDimensionPx"
        if (maxDimensionPx <= THUMBNAIL_CACHE_MAX_DIMENSION) {
            thumbnailCache.get(cacheKey)?.let { return it }
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                val file = File(localPath)
                if (!file.exists() || !file.isFile) return@withContext null

                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(localPath, boundsOptions)

                val outWidth = boundsOptions.outWidth
                val outHeight = boundsOptions.outHeight
                if (outWidth <= 0 || outHeight <= 0) return@withContext null

                var inSampleSize = 1
                while (
                    ((outWidth / inSampleSize) > maxDimensionPx || (outHeight / inSampleSize) > maxDimensionPx) &&
                    (outWidth / (inSampleSize * 2)) >= 1 &&
                    (outHeight / (inSampleSize * 2)) >= 1
                ) {
                    inSampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeFile(localPath, decodeOptions) ?: return@withContext null

                val orientation = runCatching {
                    ExifInterface(localPath).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        matrix.postRotate(90f)
                        matrix.postScale(-1f, 1f)
                    }
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        matrix.postRotate(270f)
                        matrix.postScale(-1f, 1f)
                    }
                }

                val oriented = if (!matrix.isIdentity) {
                    val transformed = Bitmap.createBitmap(
                        decoded,
                        0,
                        0,
                        decoded.width,
                        decoded.height,
                        matrix,
                        true
                    )
                    if (transformed != decoded) {
                        decoded.recycle()
                    }
                    transformed
                } else {
                    decoded
                }

                oriented.asImageBitmap().also { imageBitmap ->
                    if (maxDimensionPx <= THUMBNAIL_CACHE_MAX_DIMENSION) {
                        thumbnailCache.put(cacheKey, imageBitmap)
                    }
                }
            }.getOrNull()
        }
    }

    fun deleteLocalFile(attachment: TaskAttachment) {
        runCatching {
            val prefix = "${attachment.localPath}@"
            thumbnailCache.snapshot().keys
                .filter { it.startsWith(prefix) }
                .forEach { thumbnailCache.remove(it) }
            val file = File(attachment.localPath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun formatFileSize(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        return when {
            safeBytes < 1024L -> "$safeBytes Б"
            safeBytes < 1024L * 1024L -> "${safeBytes / 1024L} КБ"
            safeBytes < 1024L * 1024L * 1024L -> {
                val mb = safeBytes / (1024.0 * 1024.0)
                String.format(Locale.US, "%.1f МБ", mb)
            }
            else -> {
                val gb = safeBytes / (1024.0 * 1024.0 * 1024.0)
                String.format(Locale.US, "%.1f ГБ", gb)
            }
        }
    }
}
