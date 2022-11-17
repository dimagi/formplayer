package org.commcare.formplayer.tests

import org.commcare.formplayer.beans.NewFormResponse
import org.commcare.formplayer.beans.menus.EntityListResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

@WebMvcTest
class CaseListAutoSelectTests : BaseTestClass() {

    companion object {
        private const val APP = "case_list_auto_select"
    }

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        configureRestoreFactory("case_list_auto_selectdomain", "case_list_auto_selectusername")
    }

    override fun getMockRestoreFileName(): String {
        return "restores/single_case.xml"
    }

    @Test
    fun testAutoSelection() {
        // We directly go to the form without selecting the case
        val selections = arrayOf("0", "2")
        sessionNavigate(selections, APP, NewFormResponse::class.java)
    }

    @Test
    fun testAutoSelectionWithDetailScreen() {
        // We don't auto select when detail screen is defined for single select case lists
        val selections = arrayOf("0", "1")
        sessionNavigate(selections, APP, EntityListResponse::class.java)
    }
}
