package org.commcare.formplayer.util

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMultimap
import org.commcare.session.SessionFrame
import org.commcare.suite.model.StackFrameStep
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class CaseSearchNavigationUtilsTest {

    @Test
    fun mergeQueryParamsWithQueryExtras() {
        val queryParams = ArrayListMultimap.create<String, String>()
        val queryExtras = ArrayListMultimap.create<String, String>()

        queryParams.put("param1", "1A")
        queryParams.put("param1", "1B")
        queryParams.put("param2", "2A")
        queryParams.put("param3", "3A")

        queryExtras.put("param1", "1A")
        queryExtras.put("param2", "2B")
        queryExtras.put("param4", "4A")

        // merged map with unique values per key
        val expectedQueryParams =
            ImmutableMultimap.builder<String, String>().put("param1", "1A").put("param1", "1B").put("param2", "2A")
                .put("param2", "2B").put("param3", "3A").put("param4", "4A").build()

        // extras remains unchanged
        val expectedQueryExtras = ImmutableMultimap.builder<String, String>().putAll(queryExtras).build()

        CaseSearchNavigationUtils.mergeQueryParamsWithQueryExtras(queryParams, queryExtras)
        assertEquals(expectedQueryParams, queryParams)
        assertEquals(expectedQueryExtras, queryExtras)
    }

    @Test
    fun getQueryDataFromFrame() {
        val sessionFrameWithQueryExtras = getEndpointFrameWithQueryExtras()

        // Create another session frame correspoding to manual navigation to a inline search form
        // as [m3, query:results, case_id, m3-f0] but without any query extras
        val currentSessionFrame = SessionFrame()
        currentSessionFrame.pushStep(StackFrameStep(SessionFrame.STATE_COMMAND_ID, "m3", null))
        currentSessionFrame.pushStep(StackFrameStep(SessionFrame.STATE_QUERY_REQUEST, "results", "url"))
        currentSessionFrame.pushStep(StackFrameStep(SessionFrame.STATE_DATUM_VAL, "case_id", "case_id_1"))
        currentSessionFrame.pushStep(StackFrameStep(SessionFrame.STATE_COMMAND_ID, "m3-f0", null))

        val queryData =
            CaseSearchNavigationUtils.getQueryDataFromFrame(currentSessionFrame, sessionFrameWithQueryExtras)
        val queryDataExtras = queryData.getExtras("m3_results")
        assertEquals(queryDataExtras.get("case_type"), ImmutableList.of("case"));
        assertEquals(queryDataExtras.get("case_id"), ImmutableList.of("case_id_1"));
    }

    @Test
    fun getQueryDataFromFrame_currentSesionHasNoQuery() {
        val sessionFrameWithQueryExtras = getEndpointFrameWithQueryExtras()

        // current frame with no query steps
        val currentSessionFrame = SessionFrame()
        currentSessionFrame.pushStep(StackFrameStep(SessionFrame.STATE_COMMAND_ID, "m3", null))
        currentSessionFrame.pushStep(StackFrameStep(SessionFrame.STATE_DATUM_VAL, "case_id", "case_id_1"))
        currentSessionFrame.pushStep(StackFrameStep(SessionFrame.STATE_COMMAND_ID, "m3-f0", null))

        val queryData =
            CaseSearchNavigationUtils.getQueryDataFromFrame(currentSessionFrame, sessionFrameWithQueryExtras)

        // query data should be empty as there are no query steps in the current frame
        assertEquals(queryData.keys.size, 0)
    }

    private fun getEndpointFrameWithQueryExtras(): SessionFrame {
        /**
         * create a session frame corresponding to endpoint stack -
         * <push>
         *         <command value="'m3'"/>
         *         <query id="results" value="url">
         *           <data key="case_type" ref="'case'"/>
         *           <data key="case_id" ref="case_id_1"/>
         *         </query>
         *         <datum id="case_id" value="case_id_1"/>
         *         <command value="'m3-f0'"/>
         * </push>
         */
        var sessionFrameWithQueryExtras = SessionFrame()
        sessionFrameWithQueryExtras.pushStep(StackFrameStep(SessionFrame.STATE_COMMAND_ID, "m3", null))
        var queryStep = StackFrameStep(SessionFrame.STATE_QUERY_REQUEST, "results", "url")
        queryStep.addExtra("case_type", "case")
        queryStep.addExtra("case_id", "case_id_1")
        sessionFrameWithQueryExtras.pushStep(queryStep)
        sessionFrameWithQueryExtras.pushStep(StackFrameStep(SessionFrame.STATE_DATUM_VAL, "case_id", "case_id_1"))
        sessionFrameWithQueryExtras.pushStep(StackFrameStep(SessionFrame.STATE_COMMAND_ID, "m3-f0", null))
        return sessionFrameWithQueryExtras
    }
}
