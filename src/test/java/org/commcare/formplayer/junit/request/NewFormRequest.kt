package org.commcare.formplayer.junit.request

import org.commcare.formplayer.beans.NewFormResponse
import org.commcare.formplayer.beans.NewSessionRequestBean
import org.commcare.formplayer.util.Constants
import org.commcare.formplayer.utils.FileUtils
import org.commcare.formplayer.web.client.WebClient
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.test.web.servlet.MockMvc

/**
 * Request class for making a mock request that starts a new form session.
 */
class NewFormRequest(
    mockMvc: MockMvc,
    private val webClientMock: WebClient,
    private val formPath: String
    ) : MockRequest<NewSessionRequestBean, NewFormResponse>(
    mockMvc, Constants.URL_NEW_SESSION, NewFormResponse::class
) {

    fun request(requestPath: String): Response<NewFormResponse> {
        val requestPayload = FileUtils.getFile(this.javaClass, requestPath)
        val newSessionRequestBean = mapper.readValue(
            requestPayload,
            NewSessionRequestBean::class.java
        )
        return requestWithBean(newSessionRequestBean)
    }

    override fun requestWithBean(requestBean: NewSessionRequestBean): Response<NewFormResponse> {
        Mockito.`when`(webClientMock.get(ArgumentMatchers.anyString()))
            .thenReturn(FileUtils.getFile(this.javaClass, formPath))
        return super.requestWithBean(requestBean)
    }
}
