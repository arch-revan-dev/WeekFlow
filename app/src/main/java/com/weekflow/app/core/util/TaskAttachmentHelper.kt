package com.weekflow.app.core.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.weekflow.app.domain.model.TaskAttachment
import java.io.File
import java.util.Locale
import java.util.UUID

object TaskAttachmentHelper {

    private const val ATTACHMENTS_DIR = "attachments"

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
            val fallbackExt = rawMimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
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
                ?: displayName.substringAfterLast('.', "")
                    .lowercase(Locale.US)
                    .takeIf { it.isNotBlank() }
                    ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                ?: "application/octet-stream"

            val attachmentsDir = File(context.filesDir, ATTACHMENTS_DIR)
            if (!attachmentsDir.exists()) {
                attachmentsDir.mkdirs()
            }

            val attachmentId = UUID.randomUUID().toString()
            val sanitizedName = displayName
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .takeLast(80)
                .ifBlank { "attachment" }
            val destFile = File(attachmentsDir, "${attachmentId}_$sanitizedName")

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

    fun deleteLocalFile(attachment: TaskAttachment) {
        runCatching {
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
