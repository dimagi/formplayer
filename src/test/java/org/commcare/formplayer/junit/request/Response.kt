package org.commcare.formplayer.junit.request

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultHandler
import org.springframework.test.web.servlet.ResultMatcher
import kotlin.reflect.KClass

/**
 * Wrapper class for MockMVC responses that gives access ot the response bean as well as the
 * ResultActions object.
 */
class Response<T : Any>(
    private val mapper: ObjectMapper,
    val response: ResultActions,
    private val kClass: KClass<T>
) : ResultActions {

    fun bean(): T {
        return mapper.readValue(
            response.andReturn().response.contentAsString, kClass.java
        )
    }

    override fun andExpect(matcher: ResultMatcher): ResultActions {
        return response.andExpect(matcher)
    }

    override fun andDo(handler: ResultHandler): ResultActions {
        return response.andDo(handler)
    }

    override fun andReturn(): MvcResult {
        return response.andReturn()
    }
}
