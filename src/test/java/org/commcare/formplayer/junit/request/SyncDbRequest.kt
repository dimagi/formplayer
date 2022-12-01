package org.commcare.formplayer.junit.request

import org.commcare.formplayer.beans.SyncDbRequestBean
import org.commcare.formplayer.beans.SyncDbResponseBean
import org.commcare.formplayer.services.RestoreFactory
import org.commcare.formplayer.util.Constants
import org.springframework.test.web.servlet.MockMvc

/**
 * Request class for making a mock syncdb request
 */
class SyncDbRequest(
    mockMvc: MockMvc,
    private val restoreFactory: RestoreFactory
    ) : MockRequest<SyncDbRequestBean, SyncDbResponseBean>(
    mockMvc, Constants.URL_SYNC_DB, SyncDbResponseBean::class
) {

    fun request(): Response<SyncDbResponseBean> {
        val bean = SyncDbRequestBean()
        bean.domain = restoreFactory.domain
        bean.username = restoreFactory.username
        bean.restoreAs = restoreFactory.asUsername
        return requestWithBean(bean)
    }
}
