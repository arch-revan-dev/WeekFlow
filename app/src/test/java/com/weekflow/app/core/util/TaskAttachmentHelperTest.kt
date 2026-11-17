package com.weekflow.app.core.util

import com.weekflow.app.domain.model.TaskAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskAttachmentHelperTest {

    private fun attachment(
        displayName: String = "file",
        localPath: String = "/data/user/0/com.weekflow.app/files/attachments/file",
        mimeType: String = ""
    ) = TaskAttachment(
        id = "att-1",
        displayName = displayName,
        localPath = localPath,
        mimeType = mimeType,
        sizeBytes = 1024L
    )

    @Test
    fun `isImageAttachment detects case-insensitive extensions, mime types, and rejects non-images`() {
        assertTrue(
            TaskAttachmentHelper.isImageAttachment(
                attachment(displayName = "PHOTO.JPG", mimeType = "application/octet-stream")
            )
        )
        assertTrue(
            TaskAttachmentHelper.isImageAttachment(
                attachment(displayName = "diagram.png", mimeType = "")
            )
        )
        assertTrue(
            TaskAttachmentHelper.isImageAttachment(
                attachment(displayName = "IMG_123", mimeType = "image/png")
            )
        )
        assertTrue(
            TaskAttachmentHelper.isImageAttachment(
                attachment(displayName = "IMG_123", localPath = "/tmp/cache/shot.WEBP", mimeType = "")
            )
        )
        assertFalse(
            TaskAttachmentHelper.isImageAttachment(
                attachment(displayName = "syllabus.pdf", localPath = "/tmp/syllabus.pdf", mimeType = "application/pdf")
            )
        )
        assertFalse(
            TaskAttachmentHelper.isImageAttachment(
                attachment(displayName = "notes.txt", localPath = "/tmp/notes.txt", mimeType = "text/plain")
            )
        )
    }

    @Test
    fun `resolveImageMimeType resolves extensions, explicit image mime, and wildcard fallback`() {
        assertEquals(
            "image/jpeg",
            TaskAttachmentHelper.resolveImageMimeType(
                attachment(displayName = "PHOTO.JPG", mimeType = "application/octet-stream")
            )
        )
        assertEquals(
            "image/heic",
            TaskAttachmentHelper.resolveImageMimeType(
                attachment(displayName = "IMG_001.heic", mimeType = "image/*")
            )
        )
        assertEquals(
            "image/jpeg",
            TaskAttachmentHelper.resolveImageMimeType(
                attachment(displayName = "IMG_123", localPath = "/tmp/IMG_123", mimeType = "image/jpeg")
            )
        )
        assertEquals(
            "image/webp",
            TaskAttachmentHelper.resolveImageMimeType(
                attachment(displayName = "photo.jpg", mimeType = " IMAGE/WEBP ")
            )
        )
        assertEquals(
            "image/png",
            TaskAttachmentHelper.resolveImageMimeType(
                attachment(displayName = "IMG_123", localPath = "/tmp/stored.PNG", mimeType = "image/*")
            )
        )
        assertEquals(
            "image/*",
            TaskAttachmentHelper.resolveImageMimeType(
                attachment(displayName = "IMG_123", localPath = "/tmp/IMG_123", mimeType = "image/*")
            )
        )
    }

    @Test
    fun `buildStorageFileName normalizes uppercase extensions and appends extension from image mime`() {
        assertEquals(
            "PHOTO.jpg",
            TaskAttachmentHelper.buildStorageFileName("PHOTO.JPG", "image/jpeg")
        )
        assertEquals(
            "IMG_123.jpg",
            TaskAttachmentHelper.buildStorageFileName("IMG_123", "image/jpeg")
        )
        assertEquals(
            "scan.png",
            TaskAttachmentHelper.buildStorageFileName("scan.", "image/png")
        )
        assertEquals(
            "attachment",
            TaskAttachmentHelper.buildStorageFileName("   ", null)
        )
    }

    @Test
    fun `formatFileSize formats bytes, kilobytes, megabytes, and gigabytes`() {
        assertEquals("0 Б", TaskAttachmentHelper.formatFileSize(-50L))
        assertEquals("0 Б", TaskAttachmentHelper.formatFileSize(0L))
        assertEquals("512 Б", TaskAttachmentHelper.formatFileSize(512L))
        assertEquals("1 КБ", TaskAttachmentHelper.formatFileSize(1024L))
        assertEquals("1 КБ", TaskAttachmentHelper.formatFileSize(1536L))
        assertEquals("1.0 МБ", TaskAttachmentHelper.formatFileSize(1024L * 1024L))
        assertEquals("2.5 МБ", TaskAttachmentHelper.formatFileSize((2.5 * 1024 * 1024).toLong()))
        assertEquals("1.0 ГБ", TaskAttachmentHelper.formatFileSize(1024L * 1024L * 1024L))
    }
}
