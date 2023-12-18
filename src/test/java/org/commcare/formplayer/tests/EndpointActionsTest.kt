package org.commcare.formplayer.tests

import org.commcare.formplayer.application.MenuController
import org.commcare.formplayer.beans.menus.BaseResponseBean
import org.commcare.formplayer.beans.menus.EntityListResponse
import org.commcare.formplayer.configuration.CacheConfiguration
import org.commcare.formplayer.junit.*
import org.commcare.formplayer.junit.request.SessionNavigationRequest
import org.commcare.formplayer.utils.TestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest
@Import(MenuController::class)
@ContextConfiguration(classes = [TestContext::class, CacheConfiguration::class])
@ExtendWith(InitializeStaticsExtension::class)
class EndpointActionsTest {

    companion object {
        const val APP_NAME = "multi_select_case_list"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @RegisterExtension
    var restoreFactoryExt = RestoreFactoryExtension.builder()
        .withUser("test").withDomain("test")
        .withRestorePath("test_restore.xml")
        .build()

    @RegisterExtension
    var storageExt = StorageFactoryExtension.builder()
        .withUser("test").withDomain("test").build()

    @Test
    fun testEndpointActionResponse() {
        val selections = arrayOf("0", "1")
        val entityListResponse: EntityListResponse = navigate(
            selections,
            EntityListResponse::class.java
        )
        val endpoitnActionsResponse = entityListResponse.endpointActions
        assertEquals(endpoitnActionsResponse.size, 2)
        assertEquals("/a/{domain}/app/v1/{appid}/case_list/?selected_cases={selected_cases}", endpoitnActionsResponse[0].urlTemplate)
        assertEquals(true, endpoitnActionsResponse[0].isBackground)
        assertNull(endpoitnActionsResponse[1], "Endpoint Action response for fields with no endpoint_action should be null")
    }

    private fun <T : BaseResponseBean> navigate(
        selections: Array<String>, responseClass: Class<T>
    ): T {
        val installReference = Installer.getInstallReference(APP_NAME)
        val request = SessionNavigationRequest(
            mockMvc, responseClass, installReference
        )
        val bean = request.getNavigationBean(selections)
        return request.requestWithBean(bean).bean()
    }
}
