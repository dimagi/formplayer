package org.commcare.formplayer.tests

import org.commcare.formplayer.beans.NewFormResponse
import org.commcare.formplayer.beans.menus.EntityListResponse
import org.junit.jupiter.api.Assertions.assertEquals
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
        var response = sessionNavigate(selections, APP, NewFormResponse::class.java)

        // test breadcrumb
        assertEquals( 3, response.breadcrumbs.size)
        assertEquals( "Untitled Application", response.breadcrumbs[0])
        assertEquals( "Case List", response.breadcrumbs[1])
        assertEquals( "Followup Form 1", response.breadcrumbs[2])

        // test persistent menu
        val persistentMenu = response.persistentMenu
        assertEquals(2, persistentMenu.size)
        assertEquals("Case List", persistentMenu[0].displayText)
        assertEquals("Case List 1", persistentMenu[1].displayText)
        val zeroSelectionMenu = persistentMenu[0].commands
        assertEquals(3, zeroSelectionMenu.size)
        assertEquals("Registration Form", zeroSelectionMenu[0].displayText)
        assertEquals("Followup Form", zeroSelectionMenu[1].displayText)
        assertEquals("Followup Form 1", zeroSelectionMenu[2].displayText)
    }

    @Test
    fun testAutoSelectionWithDetailScreen() {
        // We don't auto select when detail screen is defined for single select case lists
        val selections = arrayOf("0", "1")
        sessionNavigate(selections, APP, EntityListResponse::class.java)
    }
}
