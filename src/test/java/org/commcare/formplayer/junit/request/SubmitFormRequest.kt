package org.commcare.formplayer.junit.request

import org.commcare.formplayer.beans.SubmitRequestBean
import org.commcare.formplayer.beans.SubmitResponseBean
import org.commcare.formplayer.util.Constants
import org.commcare.formplayer.utils.FileUtils
import org.springframework.test.web.servlet.MockMvc

/**
 * Request class for making a mock request submits a form.
 */
class SubmitFormRequest(mockMvc: MockMvc) : MockRequest<SubmitRequestBean, SubmitResponseBean>(
    mockMvc, Constants.URL_SUBMIT_FORM, SubmitResponseBean::class.java
) {

    fun request(sessionId: String, answers: Map<String, Any>, prevalidated: Boolean): Response<SubmitResponseBean> {
        val bean = SubmitRequestBean()
        bean.sessionId = sessionId
        bean.answers = answers
        bean.isPrevalidated = prevalidated
        return requestWithBean(bean);
    }

    fun request(requestPath: String, sessionId: String): Response<SubmitResponseBean> {
        val requestPayload = FileUtils.getFile(this.javaClass, requestPath)
        val bean = mapper.readValue(requestPayload, SubmitRequestBean::class.java)
        bean.sessionId = sessionId
        return requestWithBean(bean)
    }
}
