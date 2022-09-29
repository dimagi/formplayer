package org.commcare.formplayer.junit.request

import com.fasterxml.jackson.databind.ObjectMapper
import org.commcare.formplayer.beans.SubmitRequestBean
import org.commcare.formplayer.beans.SubmitResponseBean
import org.commcare.formplayer.util.Constants
import org.commcare.formplayer.utils.FileUtils
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.servlet.http.Cookie

/**
 * Request class for making a mock request submits a form.
 */
class SubmitFormRequest(private val mockMvc: MockMvc) {

    private val mapper = ObjectMapper()

    fun request(requestPath: String, sessionId: String): Response<SubmitResponseBean> {
        val requestPayload = FileUtils.getFile(this.javaClass, requestPath)
        val bean = mapper.readValue(requestPayload, SubmitRequestBean::class.java)
        bean.sessionId = sessionId
        return requestWithBean(bean)
    }

    fun requestWithBean(requestBean: SubmitRequestBean): Response<SubmitResponseBean> {
        val response = mockMvc.perform(
            post("/" + Constants.URL_SUBMIT_FORM)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(SecurityMockMvcRequestPostProcessors.user("user"))
                .content(mapper.writeValueAsString(requestBean))
        ).andExpect(MockMvcResultMatchers.status().isOk)
        return Response(mapper, response, SubmitResponseBean::class)
    }
}
