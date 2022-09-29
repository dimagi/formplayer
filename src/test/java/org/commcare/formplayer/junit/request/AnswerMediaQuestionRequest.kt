package org.commcare.formplayer.junit.request

import com.fasterxml.jackson.databind.ObjectMapper
import org.commcare.formplayer.beans.AnswerQuestionRequestBean
import org.commcare.formplayer.beans.FormEntryResponseBean
import org.commcare.formplayer.objects.SerializableFormSession
import org.commcare.formplayer.services.FormSessionService
import org.commcare.formplayer.util.Constants
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockPart
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.servlet.http.Cookie

/**
 * Request class for making a mock request that answers a media form question.
 */
class AnswerMediaQuestionRequest(
    private val mockMvc: MockMvc,
    private val formSessionService: FormSessionService
) {

    private val mapper = ObjectMapper()

    fun request(
        questionIndex: String,
        file: MockMultipartFile,
        sessionId: String
    ): Response<FormEntryResponseBean> {
        val bean = AnswerQuestionRequestBean(questionIndex, null, sessionId)
        val session: SerializableFormSession = formSessionService.getSessionById(sessionId)
        bean.username = session.username
        bean.domain = session.domain
        bean.sessionId = sessionId
        return requestWithBean(bean, file)
    }

    fun requestWithBean(
        requestBean: AnswerQuestionRequestBean,
        file: MockMultipartFile
    ): Response<FormEntryResponseBean> {
        val answer = MockPart(Constants.PART_ANSWER, mapper.writeValueAsBytes(requestBean))
        answer.headers.contentType = MediaType.APPLICATION_JSON

        val response = mockMvc.perform(
            multipart("/" + Constants.URL_ANSWER_MEDIA_QUESTION)
                .file(file)
                .part(answer)
                .cookie(Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(SecurityMockMvcRequestPostProcessors.user("user"))
        ).andExpect(MockMvcResultMatchers.status().isOk)
        return Response(mapper, response, FormEntryResponseBean::class)
    }
}
