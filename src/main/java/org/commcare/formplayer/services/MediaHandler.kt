package org.commcare.formplayer.services

import com.google.common.collect.ImmutableList
import org.commcare.util.FileUtils
import org.javarosa.core.services.locale.Localization
import org.javarosa.core.util.PropertyUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.URLConnection
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Supporting methods to process and save media files on the filesystem
 */
class MediaHandler(val file: MultipartFile) {

    companion object {
        // 3 MB size limit
        const val MAX_BYTES = (3 * 1048576 - 1024).toLong()
        private val SUPPORTED_FILE_EXTS = ImmutableList.of(".jpg", "jpeg", "png", "pdf")
        private val SUPPORTED_MIME_TYPES = ImmutableList.of("image", "application/pdf")
    }

    /**
     * Saves file in the given parent directory
     */
    fun saveFile(parentDirPath: Path): String {
        validateFile()
        val fileId = PropertyUtils.genUUID()
        val parent = parentDirPath.toFile()
        parent.mkdirs()
        val desintationFile = getMediaFilePath(parentDirPath, fileId).toFile()
        try {
            FileUtils.copyFile(file.inputStream, desintationFile)
            return fileId
        } catch (e: IOException) {
            throw IOException("Could not copy file to destination due to " + e.message, e)
        }
    }

    private fun validateFile() {
        if (isUnSupportedFileExtension() && isUnsupportedMimeType()) {
            val unsupportedFileExtError = Localization.get("form.attachment.invalid")
            throw RuntimeException(unsupportedFileExtError)
        } else if (isFileTooLarge()) {
            val fileOversizeError = Localization.get("file.oversize.error.message")
            throw RuntimeException(fileOversizeError)
        }
    }

    private fun isUnsupportedMimeType(): Boolean {
        val mimeType: String? = URLConnection.guessContentTypeFromName(file.name)
        return mimeType.isNullOrBlank() ||
            SUPPORTED_MIME_TYPES.find { mimeType.startsWith(it) }.isNullOrBlank()
    }

    fun getMediaFilePath(parentDirPath: Path, fileId: String): Path {
        return Paths.get(parentDirPath.toString(), fileId)
    }

    fun cleanMedia(parentDirPath: Path, fileId: String): Boolean {
        val currentMedia = getMediaFilePath(parentDirPath, fileId).toFile()
        return currentMedia.delete()
    }

    private fun isUnSupportedFileExtension(): Boolean {
        return SUPPORTED_FILE_EXTS.find { file.originalFilename.endsWith(it, true) }
            .isNullOrBlank()
    }

    private fun isFileTooLarge(): Boolean {
        return file.size > MAX_BYTES
    }
}
