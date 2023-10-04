package org.commcare.formplayer.tests

import org.commcare.formplayer.beans.menus.EntityListResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

@WebMvcTest
class EndpointActionsTest : BaseTestClass() {

    companion object {
        const val APP_NAME = "multi_select_case_list"
    }

    @Test
    fun testEndpointActionResponse() {
        val selections = arrayOf("0", "1")
        val entityListResponse: EntityListResponse = sessionNavigate(
            selections,
            APP_NAME,
            EntityListResponse::class.java
        )
        val endpoitnActionsResponse = entityListResponse.endpointActions
        assertEquals(endpoitnActionsResponse.size, 2)
        assertEquals("/a/{domain}/app/v1/{appid}/case_list/?selected_cases={selected_cases}", endpoitnActionsResponse[0].urlTemplate)
        assertEquals(true, endpoitnActionsResponse[0].isBackground)
        assertNull(endpoitnActionsResponse[1], "Endpoint Action response for fields with no endpoint_action should be null")
    }
}
