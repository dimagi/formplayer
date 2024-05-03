package org.commcare.formplayer.util

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import org.commcare.formplayer.objects.QueryData
import org.commcare.session.SessionFrame

// Utility methods for managing different bits related to Case Search workflows
object CaseSearchNavigationUtils {

    @JvmStatic
    fun mergeQueryParamsWithQueryExtras(
        queryParams: Multimap<String, String>,
        queryExtras: Multimap<String, String>
    ) {
        for (key in queryExtras.keySet()) {
            val queryExtrasForKey = queryExtras[key]
            val queryParamsForKey = queryParams[key]
            for (extra in queryExtrasForKey) {
                if (!queryParamsForKey.contains(extra)) {
                    queryParams.put(key, extra)
                }
            }
        }
    }

    // Builds Query Data extras using session built using the endpoint
    @JvmStatic
    fun getQueryDataFromFrame(currentFrame: SessionFrame, endpointSessionFrame: SessionFrame): QueryData {
        val queryData = QueryData()
        var lastCommandId: String? = null
        for (step in currentFrame.steps) {
            if (step.type.contentEquals(SessionFrame.STATE_QUERY_REQUEST) && lastCommandId != null) {
                for (frameStep in endpointSessionFrame.steps) {
                    if (frameStep.id.contentEquals(step.id)) {
                        val queryKey = lastCommandId + "_" + step.id
                        val queryExtras = ImmutableMultimap.builder<String, String>()
                        val frameExtras = frameStep.extras
                        for (key in frameExtras.keySet()) {
                            queryExtras.putAll(key, frameExtras[key] as Collection<String>)
                        }
                        queryData.setExtras(queryKey, queryExtras.build())
                    }
                }
            } else if (step.type.contentEquals(SessionFrame.STATE_COMMAND_ID)) {
                lastCommandId = step.id
            }
        }
        return queryData
    }
}
