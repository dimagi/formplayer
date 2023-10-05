package org.commcare.formplayer.tests

import org.commcare.formplayer.beans.SubmitResponseBean
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

@WebMvcTest
class AutoFormSubmissionTest : BaseTestClass() {

    companion object {
        private const val APP = "multi_select_case_list"
    }

    @Test
    fun testAutoSelection() {
        // We directly go to the form without selecting the case
        val selections = arrayOf("2", "0")
        val response = sessionNavigate(selections, APP, SubmitResponseBean::class.java)
        assertNotNull(response)
        assertEquals("success", response.status)
    }
}
