package org.commcare.formplayer.junit.request

import org.commcare.formplayer.beans.SessionNavigationBean
import org.commcare.formplayer.beans.menus.BaseResponseBean
import org.commcare.formplayer.junit.Installer.Companion.mockInstallReference
import org.commcare.formplayer.util.Constants
import org.springframework.test.web.servlet.MockMvc

/**
 * Request class for making navigation requests.
 *
 * This class is generically typed to allow it to return different
 * response types depending on the navigation.
 *
 * Basic usage:
 * <pre>
 *     Response<CommandListResponseBean> response = new SessionNavigationRequest<>(
 *         mockMvc, CommandListResponseBean.class).request(selectionsArray);
 *     CommandListResponseBean responseBean = response.bean();
 * </pre>
 *
 * Typically you will need to mock the app install reference as well. This can be done
 * by passing the install reference:
 * <pre>
 *     installRef = Installer.getInstallReference("basic")
 *     response = SessionNavigationRequest(
 *         mockMvc, CommandListResponseBean.class, installRef
 *     ).request(selectionsArray)
 *     responseBean: CommandListResponseBean = response.bean()
 * </pre>
 */
class SessionNavigationRequest<out T : BaseResponseBean>(
    mockMvc: MockMvc,
    kClass: Class<T>,
    val installReference: String? = null
) : MockRequest<SessionNavigationBean, T>(
    mockMvc, Constants.URL_MENU_NAVIGATION, kClass
) {

    fun request(selections: Array<String>): Response<T> {
        val bean = getNavigationBean(selections)
        return requestWithBean(bean)
    }

    override fun requestWithBean(requestBean: SessionNavigationBean): Response<T> {
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
