package org.commcare.formplayer.junit.request

import com.fasterxml.jackson.databind.ObjectMapper
import org.commcare.formplayer.util.Constants
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.servlet.http.Cookie

/**
 * Base class for mock requests
 */
open class MockRequest<B, out T : Any>(
    private val mockMvc: MockMvc,
    private val requestPath: String,
    private val kClass: Class<T>
) {

    internal val mapper = ObjectMapper()
    open val defaultExpectations = arrayOf(MockMvcResultMatchers.status().isOk)

    open fun requestWithBean(requestBean: B): Response<T> {
        var path = requestPath
        if (!path.startsWith("/")) {
            path = "/$path"
        }
        val requestBuilder = getRequestBuilder(path, requestBean)
        val response = mockMvc.perform(requestBuilder).andExpectAll(*defaultExpectations)
        return Response(mapper, response, kClass)
    }

    open fun getRequestBuilder(requestPath: String, requestBean: B): MockHttpServletRequestBuilder {
        return post(requestPath)
            .contentType(MediaType.APPLICATION_JSON)
            .cookie(Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .with(SecurityMockMvcRequestPostProcessors.user("user"))
            .content(mapper.writeValueAsString(requestBean))
    }
}
