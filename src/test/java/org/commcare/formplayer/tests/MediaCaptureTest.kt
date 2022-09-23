package org.commcare.formplayer.tests

import org.commcare.formplayer.application.FormController
import org.commcare.formplayer.application.FormSubmissionController
import org.commcare.formplayer.beans.FormEntryResponseBean
import org.commcare.formplayer.beans.NewFormResponse
import org.commcare.formplayer.configuration.CacheConfiguration
import org.commcare.formplayer.junit.FormSessionTest
import org.commcare.formplayer.junit.RestoreFactoryExtension
import org.commcare.formplayer.junit.StorageFactoryExtension
import org.commcare.formplayer.junit.request.AnswerMediaQuestionRequest
import org.commcare.formplayer.junit.request.NewFormRequest
import org.commcare.formplayer.junit.request.SubmitFormRequest
import org.commcare.formplayer.services.FormSessionService
import org.commcare.formplayer.services.SubmitService
import org.commcare.formplayer.util.Constants.PART_FILE
import org.commcare.formplayer.utils.FileUtils
import org.commcare.formplayer.utils.TestContext
import org.commcare.formplayer.web.client.WebClient
import org.javarosa.core.services.locale.Localization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.fail
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.util.AssertionErrors.assertFalse
import org.springframework.test.util.AssertionErrors.assertTrue
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@WebMvcTest
@ContextConfiguration(classes = [TestContext::class, CacheConfiguration::class])
@Import(FormController::class, FormSubmissionController::class)
@FormSessionTest
class MediaCaptureTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webClientMock: WebClient

    @Autowired
    private lateinit var submitServiceMock: SubmitService

    @Autowired
    private lateinit var formSessionService: FormSessionService

    companion object {
        const val USERNAME = "test"
        const val DOMAIN = "test"
        const val APP_ID = "10a706429116a3e55f1d1302cd3db69f"
        const val IMAGE_CAPTURE_INDEX = 21

        @JvmField
        @RegisterExtension
        val restoreFactoryExt = RestoreFactoryExtension.builder()
            .withUser(USERNAME).withDomain(DOMAIN)
            .build()

        @JvmField
        @RegisterExtension
        val storageFactoryExt = StorageFactoryExtension.builder()
            .withAppId(APP_ID).withUser(USERNAME).withDomain(DOMAIN)
            .build()
    }


    @Test
    fun testImageCapture_fileSaveAndReplace() {
        val formResponse = startImageCaptureForm()
        var responseBean: FormEntryResponseBean
        try {
            responseBean = saveImage(formResponse, "media/valid_image.jpg", "valid_image.jpg")
        } catch (e: Exception) {
            fail("Unable to save a valid file due to " + e.message)
        }

        // get file from path and check if it's the same file
        var expectedFilePath = getExpectedMediaPath(formResponse.session_id, responseBean)
        val originalSavedFile = expectedFilePath.toFile()
        assertTrue("Could not find saved file on the filesystem", originalSavedFile.exists())

        // upload an invalid file and check the old file remains as answer
        assertThrows<java.lang.Exception> {
            saveImage(formResponse, "media/invalid_extension.jppg", "invalid_extension.jppg")
        }
        assertTrue("Originally saved file was replaced by an invalid file upload", originalSavedFile.exists())

        // Upload again and check the old file gets cleared
        responseBean = saveImage(formResponse, "media/valid_image.jpg", "valid_image.jpg")
        assertFalse("Old image is still present on the filesystem", originalSavedFile.exists())
        expectedFilePath = getExpectedMediaPath(formResponse.session_id, responseBean)
        assertTrue("Could not find saved file on the filesystem", expectedFilePath.toFile().exists())
    }

    private fun getExpectedMediaPath(sessionId: String, responseBean: FormEntryResponseBean): Path {
        val imageResponse = responseBean.tree[IMAGE_CAPTURE_INDEX]
        val fileName = (imageResponse.answer as String)
        return Paths.get("forms", USERNAME, DOMAIN, APP_ID, sessionId, "media", fileName)
    }

    @Test
    fun testImageCapture_OversizedFileErrors() {
        val formResponse = startImageCaptureForm()
        val exception = assertThrows<java.lang.Exception> {
            saveImage(formResponse, "media/oversize_image.jpeg", "oversize_image.jpeg")
        }
        val expectedErr = Localization.get("file.oversize.error.message")
        assertEquals(
            expectedErr,
            exception.cause!!.message,
            "Exception message doesn't match file oversize error message",
        )
    }

    @Test
    fun testImageCapture_FileWithInvalidExtensionErrors() {
        val formResponse = startImageCaptureForm()
        val exception = assertThrows<java.lang.Exception> {
            saveImage(formResponse, "media/invalid_extension.txt", "invalid_extension.txt")
        }
        val expectedErr = Localization.get("form.attachment.invalid")
        assertEquals(
            expectedErr,
            exception.cause!!.message,
            "Exception message doesn't match file invalid error message",
        )
    }

    @Test
    fun testFormSubmissionWithMedia()  {
        val formResponse = startImageCaptureForm()
        val imageResponse = saveImage(formResponse, "media/valid_image.jpg", "valid_image.jpg")

        //Test Submission
        SubmitFormRequest(mockMvc)
            .request("requests/submit/submit_request.json", formResponse.sessionId)
            .andExpect(jsonPath("$.status").value("success"))

        argumentCaptor<MultiValueMap<String, HttpEntity<Any>>>().apply {
            Mockito.verify(submitServiceMock).submitForm(capture(), anyString())
            val body = allValues[0] as LinkedMultiValueMap<*, *>
            assertEquals(2, body.size)
            assertTrue("Form submission doesn't contain xml file part",body.containsKey("xml_submission_file"))
            checkContentType("text/xml", body["xml_submission_file"]?.get(0) as HttpEntity<*>)

            val fileName = imageResponse.tree[IMAGE_CAPTURE_INDEX].answer as String
            assertTrue("Form submission doesn't contain media file part",body.containsKey(fileName))
            val filePart = body[fileName]?.get(0) as HttpEntity<*>
            checkContentType("image/jpeg", filePart)
            val expectedFilePath = getExpectedMediaPath(formResponse.session_id, imageResponse).toString()
            assertEquals(expectedFilePath, (filePart.body as File).path)
        }
    }

    private fun checkContentType(expectedContentType: String, filePart: HttpEntity<*>) {
        val contentType = filePart.headers["Content-Type"]
        assertEquals(expectedContentType, contentType!![0])
    }

    private fun startImageCaptureForm(): NewFormResponse {
        return NewFormRequest(mockMvc, webClientMock, "xforms/question_types.xml")
            .request("requests/new_form/new_form_2.json").bean()
    }

    private fun saveImage(
        formResponse: NewFormResponse,
        filePath: String,
        fileName: String
    ): FormEntryResponseBean {
        val questions = formResponse.tree
        assertEquals("q_image_acquire", questions[IMAGE_CAPTURE_INDEX].question_id)
        val fis = FileUtils.getFileStream(this.javaClass, filePath)
        val file = MockMultipartFile(PART_FILE, fileName, MediaType.IMAGE_JPEG_VALUE, fis)

        val questionRequest = AnswerMediaQuestionRequest(mockMvc, formSessionService)
        return questionRequest.request("" + IMAGE_CAPTURE_INDEX, file, formResponse.sessionId).bean()
    }
}
