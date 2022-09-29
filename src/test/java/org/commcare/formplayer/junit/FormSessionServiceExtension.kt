package org.commcare.formplayer.junit

import org.commcare.formplayer.exceptions.FormNotFoundException
import org.commcare.formplayer.objects.SerializableFormSession
import org.commcare.formplayer.services.FormSessionService
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils
import java.util.*

/**
 * Junit extension that configures the mock FormSessionService
 *
 * Setup mocking for the FormSessionService that allows saving and retrieving sessions.
 * The 'persisted' sessions are cleared at the start of each test.
 */
class FormSessionServiceExtension : BeforeAllCallback, BeforeEachCallback {

    private lateinit var formSessionService: FormSessionService
    private val sessionMap: MutableMap<String, SerializableFormSession> = HashMap()

    override fun beforeAll(context: ExtensionContext) {
        formSessionService = SpringExtension.getApplicationContext(context).getBean(FormSessionService::class.java)
    }

    override fun beforeEach(context: ExtensionContext?) {
        sessionMap.clear()

        Mockito.doAnswer { invocation ->
            val session = invocation.arguments[0] as SerializableFormSession
            if (session.id == null) {
                // this is normally taken care of by Hibernate
                ReflectionTestUtils.setField(session, "id", UUID.randomUUID().toString())
            }
            sessionMap[session.id] = session
            session
        }.`when`(formSessionService).saveSession(
            ArgumentMatchers.any(
                SerializableFormSession::class.java
            )
        )

        Mockito.`when`(formSessionService.getSessionById(ArgumentMatchers.anyString()))
            .thenAnswer(
                Answer { invocation ->
                    val key = invocation.arguments[0] as String
                    if (sessionMap.containsKey(key)) {
                        return@Answer sessionMap[key]
                    }
                    throw FormNotFoundException(key)
                }
            )

        Mockito.doAnswer { invocation ->
            val key = invocation.arguments[0] as String
            sessionMap.remove(key)
            null
        }.`when`(formSessionService).deleteSessionById(ArgumentMatchers.anyString())
    }
}
