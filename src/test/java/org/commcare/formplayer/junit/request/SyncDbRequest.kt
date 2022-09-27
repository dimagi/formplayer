package org.commcare.formplayer.junit.request

import com.fasterxml.jackson.databind.ObjectMapper
import org.commcare.formplayer.beans.SyncDbRequestBean
import org.commcare.formplayer.beans.SyncDbResponseBean
import org.commcare.formplayer.services.RestoreFactory
import org.commcare.formplayer.util.Constants
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.servlet.http.Cookie

/**
 * Request class for making a mock syncdb request
 */
class SyncDbRequest(private val mockMvc: MockMvc, private val restoreFactory: RestoreFactory) {

    private val mapper = ObjectMapper()

    fun request(): Response<SyncDbResponseBean> {
        val bean = SyncDbRequestBean()
        bean.domain = restoreFactory.domain
        bean.username = restoreFactory.username
        bean.restoreAs = restoreFactory.asUsername
        return requestWithBean(bean)
    }

    fun requestWithBean(requestBean: SyncDbRequestBean): Response<SyncDbResponseBean> {
        val response = mockMvc.perform(
            post("/" + Constants.URL_SYNC_DB)
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(Cookie(Constants.POSTGRES_DJANGO_SESSION_ID, "derp"))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(SecurityMockMvcRequestPostProcessors.user("user"))
                .content(mapper.writeValueAsString(requestBean))
        ).andExpect(MockMvcResultMatchers.status().isOk)
        return Response(mapper, response, SyncDbResponseBean::class)
    }

}
