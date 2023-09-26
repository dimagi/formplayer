package org.commcare.formplayer.tests

import org.commcare.formplayer.beans.NewFormResponse
import org.commcare.formplayer.beans.menus.EntityListResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

@WebMvcTest
class ClickableIconsTest : BaseTestClass() {

    companion object {
        const val APP_NAME = "multi_select_case_list"
    }

    @Test
    fun testFieldActionAsInput() {
        val selections = arrayOf("0", "1", "field_action 0")
        val formResponse: NewFormResponse = sessionNavigate(
            selections,
            APP_NAME,
            NewFormResponse::class.java
        )
        assertEquals(formResponse.title, "Registration Form")
    }

    @Test
    fun testInvalidFieldActionAsInput_showsLastValidScreen() {
        val selections = arrayOf("0", "1", "field_action 8")
        val entityListResp: EntityListResponse = sessionNavigate(
            selections,
            APP_NAME,
            EntityListResponse::class.java
        )
        assertNotNull(entityListResp)
    }
}
