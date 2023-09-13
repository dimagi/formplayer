package org.commcare.formplayer.junit.request

import org.commcare.formplayer.beans.AnswerQuestionRequestBean
import org.commcare.formplayer.beans.FormEntryResponseBean
import org.commcare.formplayer.beans.NewFormResponse
import org.commcare.formplayer.beans.NewSessionRequestBean
import org.commcare.formplayer.objects.SerializableFormSession
import org.commcare.formplayer.services.FormSessionService
import org.commcare.formplayer.util.Constants
import org.springframework.test.web.servlet.MockMvc

/**
 * Request class for making a mock request that clears the answer for a media form question.
 */
class ClearMediaQuestionRequest (
    private val mockMvc: MockMvc,
    private val formSessionService: FormSessionService
): MockRequest<AnswerQuestionRequestBean, FormEntryResponseBean>(
    mockMvc, Constants.URL_CLEAR_ANSWER, FormEntryResponseBean::class.java
) {

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
}
