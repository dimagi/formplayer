package org.commcare.formplayer.services

import org.commcare.formplayer.services.MediaValidator.isFileTooLarge
import org.commcare.formplayer.services.MediaValidator.isUnSupportedFileExtension
import org.commcare.formplayer.services.MediaValidator.isUnsupportedMimeType
import org.commcare.util.FileUtils
import org.javarosa.core.services.locale.Localization
import org.javarosa.core.util.PropertyUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Supporting methods to process and save media files on the filesystem
 */
class MediaHandler(val file: MultipartFile) {

    /**
     * Saves file in the given parent directory
     */
    fun saveFile(parentDirPath: Path): String {
        validateFile()
        val fileId = PropertyUtils.genUUID()
        val parent = parentDirPath.toFile()
        parent.mkdirs()
        var fileIdWithExt = fileId
        FileUtils.getExtension(file.originalFilename)?.let { fileIdWithExt = "$fileId.$it" }
        val desintationFile = getMediaFilePath(parentDirPath, fileIdWithExt).toFile()
        try {
            FileUtils.copyFile(file.inputStream, desintationFile)
            return fileIdWithExt
        } catch (e: IOException) {
            throw IOException("Could not copy file to destination due to " + e.message, e)
        }
    }

    private fun validateFile() {
        if (isUnSupportedFileExtension(file.originalFilename) && isUnsupportedMimeType(
                file.inputStream,
                file.name
            )
        ) {
            val unsupportedFileExtError = Localization.get("form.attachment.invalid")
            throw RuntimeException(unsupportedFileExtError)
        } else if (isFileTooLarge(file.size)) {
            val fileOversizeError = Localization.get("file.oversize.error.message")
            throw RuntimeException(fileOversizeError)
        }
    }

    fun getMediaFilePath(parentDirPath: Path, fileId: String): Path {
        return Paths.get(parentDirPath.toString(), fileId)
    }

    fun cleanMedia(parentDirPath: Path, fileId: String): Boolean {
        val currentMedia = getMediaFilePath(parentDirPath, fileId).toFile()
        return currentMedia.delete()
    }
}
