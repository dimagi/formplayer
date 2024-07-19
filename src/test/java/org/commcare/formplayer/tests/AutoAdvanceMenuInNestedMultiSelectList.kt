package org.commcare.formplayer.tests

import org.commcare.formplayer.beans.NewFormResponse
import org.commcare.formplayer.beans.menus.PersistentCommand
import org.commcare.formplayer.beans.menus.QueryResponseBean
import org.commcare.formplayer.mocks.FormPlayerPropertyManagerMock
import org.commcare.formplayer.objects.QueryData
import org.commcare.formplayer.utils.MockRequestUtils
import org.commcare.util.screen.MultiSelectEntityScreen.USE_SELECTED_VALUES
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest

/**
 * Regression test for a fixed behaviour around auto-advancing menus and nested multi-select case list
 */
@WebMvcTest
class AutoAdvanceMenuInNestedMultiSelectList : BaseTestClass() {

    private lateinit var mockRequest: MockRequestUtils

    companion object {
        private const val APP_NAME = "auto_advance_in_nested_multi_select_list"
    }

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        configureRestoreFactory("domain", "user")
        storageFactoryMock.configure("user", "domain", "app_id", "asUser")
        cacheManager.getCache("case_search").clear()
        mockRequest = MockRequestUtils(webClientMock, restoreFactoryMock)
    }

    override fun getMockRestoreFileName(): String {
        return "restores/caseclaim.xml"
    }

    @Test
    fun testAutoAdvanceMenuInNestedMultiSelectList() {
        FormPlayerPropertyManagerMock.mockAutoAdvanceMenu(storageFactoryMock)
        mockRequest.mockQuery(
            "query_responses/case_search_multi_select_response.xml", 2
        ).use {
            val queryData = QueryData()
            var response = sessionNavigateWithQuery(
                arrayOf("1", USE_SELECTED_VALUES),
                APP_NAME,
                queryData,
                arrayOf("94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18"),
                QueryResponseBean::class.java
            )

            // Peristent Menu check
            val responseSelections = response.selections.asList()
            val expectedMenu = java.util.ArrayList<PersistentCommand>()
            expectedMenu.add(
                PersistentCommand(
                    "0",
                    "Case List"
                )
            )
            expectedMenu.add(
                PersistentCommand(
                    "1",
                    "Parent Case List"
                )
            )
            val parentCaseListMenu = expectedMenu[1]
            parentCaseListMenu.addCommand(
                PersistentCommand(
                    responseSelections.last(),
                    "Batman Begins"
                )
            )
            // add the auto-advanced menu
            val batmanBeginsMenu = parentCaseListMenu.commands[0]
            batmanBeginsMenu.addCommand(
                PersistentCommand(
                    "0",
                    "Nested Case List"
                )
            )
            assertEquals(expectedMenu, response.persistentMenu)


            val updatedSelections = ArrayList<String>()
            updatedSelections.addAll(responseSelections)
            updatedSelections.add(USE_SELECTED_VALUES)
            val formResponse = sessionNavigateWithQuery(
                updatedSelections.toTypedArray(),
                APP_NAME,
                queryData,
                arrayOf("94f8d030-c6f9-49e0-bc3f-5e0cdbf10c18"),
                NewFormResponse::class.java
            )

            // selections array at this point should be ["1", "guid1", "guid2"] without any auto-advanced menu indexes
            assertEquals(formResponse.selections.size, 3)

            // Persistent Menu check
            val nestedCaseListMenu = batmanBeginsMenu.commands[0]
            nestedCaseListMenu.addCommand(
                PersistentCommand(
                    formResponse.selections.last(),
                    "Batman Begins"
                )
            )
            val nestedBatmanbeginsMenu = nestedCaseListMenu.commands[0]
            nestedBatmanbeginsMenu.addCommand(
                PersistentCommand(
                    "0",
                    "Followup"
                )
            );
            assertEquals(expectedMenu, formResponse.persistentMenu)
        }
    }
}
