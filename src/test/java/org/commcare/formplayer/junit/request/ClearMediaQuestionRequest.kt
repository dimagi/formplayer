package org.commcare.formplayer.junit.request

import com.fasterxml.jackson.databind.ObjectMapper
import org.commcare.formplayer.beans.AnswerQuestionRequestBean
import org.commcare.formplayer.beans.FormEntryResponseBean
import org.commcare.formplayer.objects.SerializableFormSession
import org.commcare.formplayer.services.FormSessionService
import org.commcare.formplayer.util.Constants
import org.springframework.http.HttpMethod
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import javax.servlet.http.Cookie

/**
 * Request class for making a mock request that clears the answer for a media form question.
 */
class ClearMediaQuestionRequest (
    private val mockMvc: MockMvc,
    private val formSessionService: FormSessionService
) {

    private val mapper = ObjectMapper()

    fun request(
        questionIndex: String,
        sessionId: String
    ): Response<FormEntryResponseBean> {
        val bean = AnswerQuestionRequestBean(questionIndex, null, sessionId)
        val session: SerializableFormSession = formSessionService.getSessionById(sessionId)
        bean.username = session.username
        bean.domain = session.domain
        bean.sessionId = sessionId
        return requestWithBean(bean)
    }

    fun requestWithBean(
        requestBean: AnswerQuestionRequestBean,
    ): Response<FormEntryResponseBean> {
        val req = MockMvcRequestBuilders.request(HttpMethod.POST, "/" + Constants.URL_CLEAR_ANSWER)
            .cookie(Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .with(SecurityMockMvcRequestPostProcessors.user("user"))
        val response = mockMvc.perform(req)
        return Response(mapper, response, FormEntryResponseBean::class.java)
    }
}