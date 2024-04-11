package org.commcare.formplayer.junit.request

import org.commcare.formplayer.beans.SessionNavigationBean
import org.commcare.formplayer.junit.Installer.Companion.mockInstallReference
import org.commcare.formplayer.util.Constants
import org.springframework.test.web.servlet.MockMvc
import org.commcare.formplayer.beans.SyncDbResponseBean

/**
 * Request class for making mock schedule syncdb requests.
 */
class ScheduleSyncDbRequest(
    mockMvc: MockMvc,
    val installReference: String? = null
) : MockRequest<SessionNavigationBean, SyncDbResponseBean>(
    mockMvc, Constants.URL_INTERVAL_SYNC_DB, SyncDbResponseBean::class.java
) {

    fun request(selections: Array<String>): Response<SyncDbResponseBean> {
        val bean = getNavigationBean(selections)
        return requestWithBean(bean)
    }

    override fun requestWithBean(requestBean: SessionNavigationBean): Response<SyncDbResponseBean> {
        return if (installReference == null) {
            super.requestWithBean(requestBean)
        } else {
            mockInstallReference(
                { super.requestWithBean(requestBean) },
                installReference
            )
        }
    }

    fun getNavigationBean(selections: Array<String>): SessionNavigationBean {
        val sessionNavigationBean = SessionNavigationBean()
        sessionNavigationBean.domain = "test-domain"
        sessionNavigationBean.appId = "test-app-id"
        sessionNavigationBean.username = "test-username"
        sessionNavigationBean.selections = selections
        return sessionNavigationBean
    }
}
