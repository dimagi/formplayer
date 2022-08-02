package org.commcare.formplayer.services

import org.commcare.util.FileUtils
import org.javarosa.core.services.locale.Localization
import org.javarosa.core.util.PropertyUtils
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.IOException

/**
 * Supporting methods to process and save media files on the filesystem
 */
class MediaHandler(val file: MultipartFile) {

    companion object {
        // 3 MB size limit
        const val MAX_BYTES = (3 * 1048576 - 1024).toLong()
        private val SUPPORTED_FILE_EXTS = arrayOf(".jpg", "jpeg", "png")
    }

    /**
     * Saves file in the given parent directory
     */
    fun saveFile(parentDir: String): String {
        validateFile()
        val fileId = PropertyUtils.genUUID()
        val parent = File(parentDir)
        parent.mkdirs()
        val mediaPath: String = getMediaFilePath(parentDir, fileId)
        try {
            FileUtils.copyFile(file.inputStream, File(mediaPath))
            return fileId
        } catch (e: IOException) {
            throw IOException("Could not copy file to destination due to " + e.message, e)
        }
    }

    private fun validateFile() {
        if (isUnSupportedFileExtension()) {
            val unsupportedFileExtError = Localization.get("form.attachment.invalid")
            throw RuntimeException(unsupportedFileExtError)
        } else if (isFileTooLarge()) {
            val fileOversizeError = Localization.get("file.oversize.error.message")
            throw RuntimeException(fileOversizeError)
        }
    }

    fun getMediaFilePath(parentDir: String, fileId: String): String {
        return parentDir + fileId
    }

    fun cleanMedia(parentDir: String, fileId: String): Boolean {
        val currentMedia = File(getMediaFilePath(parentDir, fileId))
        return currentMedia.delete()
    }

    private fun isUnSupportedFileExtension(): Boolean {
        for (supportedFileExt in SUPPORTED_FILE_EXTS) {
            if (file.originalFilename.endsWith(supportedFileExt, true)) {
                return false
            }
        }
        return true
    }

    private fun isFileTooLarge(): Boolean {
        return file.size > MAX_BYTES
    }
}
