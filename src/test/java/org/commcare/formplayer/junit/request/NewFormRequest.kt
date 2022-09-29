package org.commcare.formplayer.junit.request

import com.fasterxml.jackson.databind.ObjectMapper
import org.commcare.formplayer.beans.NewFormResponse
import org.commcare.formplayer.beans.NewSessionRequestBean
import org.commcare.formplayer.util.Constants
import org.commcare.formplayer.utils.FileUtils
import org.commcare.formplayer.web.client.WebClient
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.servlet.http.Cookie

/**
 * Request class for making a mock request that starts a new form session.
 */
class NewFormRequest(
    private val mockMvc: MockMvc,
    private val webClientMock: WebClient,
    private val formPath: String
) {

    private val mapper = ObjectMapper()

    fun request(requestPath: String): Response<NewFormResponse> {
        val requestPayload = FileUtils.getFile(this.javaClass, requestPath)
        val newSessionRequestBean = mapper.readValue(
            requestPayload,
            NewSessionRequestBean::class.java
        )
        return requestWithBean(newSessionRequestBean)
    }

    fun requestWithBean(requestBean: NewSessionRequestBean): Response<NewFormResponse> {
        Mockito.`when`(webClientMock.get(ArgumentMatchers.anyString()))
            .thenReturn(FileUtils.getFile(this.javaClass, formPath))

        val response = mockMvc.perform(
            post("/" + Constants.URL_NEW_SESSION)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(SecurityMockMvcRequestPostProcessors.user("user"))
                .content(mapper.writeValueAsString(requestBean))
        ).andExpect(MockMvcResultMatchers.status().isOk)
        return Response(mapper, response, NewFormResponse::class)
    }
}
