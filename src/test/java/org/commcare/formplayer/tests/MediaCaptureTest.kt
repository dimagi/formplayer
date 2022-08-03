package org.commcare.formplayer.tests

import org.commcare.formplayer.beans.FormEntryResponseBean
import org.commcare.formplayer.beans.NewFormResponse
import org.commcare.formplayer.utils.FileUtils
import org.javarosa.core.services.locale.Localization
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.AssertionErrors.assertFalse
import org.springframework.test.util.AssertionErrors.assertTrue
import java.io.File

@WebMvcTest
class MediaCaptureTest : BaseTestClass() {

    companion object {
        const val USERNAME = "test"
        const val DOMAIN = "test"
        const val APP_ID = "10a706429116a3e55f1d1302cd3db69f"
        const val IMAGE_CAPTURE_INDEX = 21
    }

    @Test
    fun testImageCapture_fileSaveAndReplace() {
        val formResponse = startImageCaptureForm()
        val responseBean: FormEntryResponseBean
        try {
            responseBean = saveImage(formResponse, "media/valid_image.jpg", "valid_image.jpg")
        } catch (e: Exception) {
            fail("Unable to save a valid file due to " + e.message)
        }

        // get file from path and check if it's the same file
        var imageResponse = responseBean.tree[IMAGE_CAPTURE_INDEX]
        var expectedFilePath = String.format(
            "forms/%s/%s/%s/%s/media/%s",
            USERNAME,
            DOMAIN,
            APP_ID,
            formResponse.session_id,
            imageResponse.answer
        )
        val originalSavedFile = File(expectedFilePath)
        assertTrue("Could not find saved file on the filesystem", originalSavedFile.exists())

        // upload an invalid file and check the old file remains as answer
        assertThrows<java.lang.Exception> {
            saveImage(formResponse, "media/invalid_extension.jppg", "invalid_extension.jppg")
        }
        assertTrue("Originally saved file was replaced by an invalid file upload", originalSavedFile.exists())

        // Upload again and check the old file gets cleared
        saveImage(formResponse, "media/valid_image.jpg", "valid_image.jpg")
        assertFalse("Old image is still present on the filesystem", originalSavedFile.exists())
    }

    @Test
    fun testImageCapture_OversizedFileErrors() {
        val formResponse = startImageCaptureForm()
        val exception = assertThrows<java.lang.Exception> {
            saveImage(formResponse, "media/oversize_image.jpeg", "oversize_image.jpeg")
        }
        val expectedErr = Localization.get("file.oversize.error.message")
        assertEquals(
            "Exception message doesn't match file oversize error message",
            expectedErr,
            exception.cause!!.message
        )
    }

    @Test
    fun testImageCapture_FileWithInvalidExtensionErrors() {
        val formResponse = startImageCaptureForm()
        val exception = assertThrows<java.lang.Exception> {
            saveImage(formResponse, "media/invalid_extension.jppg", "invalid_extension.jppg")
        }
        val expectedErr = Localization.get("form.attachment.invalid")
        assertEquals(
            "Exception message doesn't match file invalid error message",
            expectedErr,
            exception.cause!!.message
        )
    }

    private fun startImageCaptureForm(): NewFormResponse {
        return startNewForm(
            "requests/new_form/new_form_2.json",
            "xforms/question_types.xml"
        )
    }

    private fun saveImage(
        formResponse: NewFormResponse,
        filePath: String,
        fileName: String
    ): FormEntryResponseBean {
        val questions = formResponse.tree
        Assertions.assertEquals("q_image_acquire", questions[IMAGE_CAPTURE_INDEX].question_id)
        val fis = FileUtils.getFileStream(this.javaClass, filePath)
        val file = MockMultipartFile("file", fileName, "image/jpg", fis)
        val response = answerMediaQuestion("" + IMAGE_CAPTURE_INDEX, file, formResponse.sessionId)
        return response
    }
}
