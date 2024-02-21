package org.commcare.formplayer.tests

import org.commcare.formplayer.application.MenuController
import org.commcare.formplayer.beans.menus.BaseResponseBean
import org.commcare.formplayer.beans.menus.EntityListResponse
import org.commcare.formplayer.configuration.CacheConfiguration
import org.commcare.formplayer.junit.*
import org.commcare.formplayer.junit.Installer.Companion.getInstallReference
import org.commcare.formplayer.junit.request.SessionNavigationRequest
import org.commcare.formplayer.utils.TestContext
import org.junit.jupiter.api.Assertions.assertEquals
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
class CaseListLazyLoadingTests {

    companion object {
        const val APP_NAME = "case_claim_with_multi_select"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @RegisterExtension
    var restoreFactoryExt = RestoreFactoryExtension.builder()
        .withUser("test").withDomain("test")
        .withRestorePath("restores/caseclaim.xml")
        .build()

    @RegisterExtension
    var storageExt = StorageFactoryExtension.builder()
        .withUser("test").withDomain("test").build()


    @Test
    fun testLazyLoadingDetail_PaginatesAsNormal() {
        val selections = arrayOf("5")
        var response = navigate(selections, EntityListResponse::class.java, 0, 2)
        var entitites = response.entities
        assertEquals(response.pageCount, 4)
        assertEquals(entitites.size, 2)

        // Ensure both sort and non sort fields are populated
        var singleEntity = entitites[1]
        assertEquals(singleEntity.data[0], "Batman Begins")
        assertEquals(singleEntity.data[1], "Batman Begins")
        assertEquals(singleEntity.groupKey, "Batman Begins")
        assertEquals(singleEntity.altText.toList(), arrayOfNulls<String>(singleEntity.data.size).toList())

        response = navigate(selections, EntityListResponse::class.java, 1, 3)
        entitites = response.entities
        assertEquals(response.pageCount, 3)
        assertEquals(entitites.size, 3)
        singleEntity = entitites[2]
        assertEquals(singleEntity.data[0], "Rudolph")
        assertEquals(singleEntity.data[1], "Rudolph")
        assertEquals(singleEntity.groupKey, "Rudolph")
        assertEquals(singleEntity.altText.toList(), arrayOfNulls<String>(singleEntity.data.size).toList())
    }

    private fun <T : BaseResponseBean> navigate(
        selections: Array<String>, responseClass: Class<T>, offset: Int, casesPerPage: Int
    ): T {
        val installReference = getInstallReference(APP_NAME)
        val request = SessionNavigationRequest(
            mockMvc, responseClass, installReference
        )
        val bean = request.getNavigationBeanForPage(selections, offset, casesPerPage)
        return request.requestWithBean(bean).bean()
    }
}
