package org.commcare.formplayer.junit

import org.commcare.formplayer.objects.SerializableFormDefinition
import org.commcare.formplayer.objects.SerializableFormSession
import org.commcare.formplayer.services.FormDefinitionService
import org.commcare.formplayer.session.FormSession
import org.commcare.formplayer.util.serializer.FormDefStringSerializer
import org.javarosa.core.model.FormDef
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.springframework.cache.CacheManager
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils
import java.io.IOException
import java.util.*

/**
 * Junit extension that configures the mock FormDefinitionService
 *
 * Setup mocking for the FormDefinitionService that allows saving and retrieving form definitions.
 * The 'persisted' definitions are cleared at the start of each test.
 */
class FormDefSessionServiceExtension : BeforeAllCallback, BeforeEachCallback {

    private lateinit var formDefinitionService: FormDefinitionService
    private lateinit var cacheManager: CacheManager

    private val formDefinitionMap: MutableMap<Long, SerializableFormDefinition> = HashMap()
    private var currentFormDefinitionId = 1L

    override fun beforeAll(context: ExtensionContext) {
        formDefinitionService = SpringExtension.getApplicationContext(context).getBean(FormDefinitionService::class.java)
        cacheManager = SpringExtension.getApplicationContext(context).getBean(CacheManager::class.java)

        // manually wire this in. The autowiring doesn't work here since we've made it a mock
        ReflectionTestUtils.setField(this.formDefinitionService, "caches", cacheManager)
    }

    override fun beforeEach(context: ExtensionContext?) {
        formDefinitionMap.clear()

        Mockito.doAnswer(Answer { invocation ->
            val appId = invocation.arguments[0] as String
            val appVersion = invocation.arguments[1] as String
            val xmlns = invocation.arguments[2] as String
            for (tmp in formDefinitionMap.values) {
                if (tmp.appId == appId && tmp.formXmlns == xmlns && tmp.formVersion == appVersion) {
                    return@Answer tmp
                }
            }
            // else create a new one
            val serializedFormDef: String? = try {
                FormDefStringSerializer.serialize(invocation.arguments[3] as FormDef)
            } catch (ex: IOException) {
                "could not serialize provided form def"
            }
            val serializableFormDef = SerializableFormDefinition(
                appId, appVersion, xmlns, serializedFormDef
            )
            if (serializableFormDef.id == null) {
                // this is normally taken care of by Hibernate
                ReflectionTestUtils.setField(serializableFormDef, "id", currentFormDefinitionId)
                currentFormDefinitionId++
            }
            formDefinitionMap[serializableFormDef.id] = serializableFormDef
            serializableFormDef
        }).`when`(this.formDefinitionService).getOrCreateFormDefinition(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(
                FormDef::class.java
            )
        )

        Mockito.`when`<FormDef>(
            this.formDefinitionService.getFormDef(
                ArgumentMatchers.any(
                    SerializableFormSession::class.java
                )
            )
        ).thenCallRealMethod()
        Mockito.`when`<FormDef>(
            this.formDefinitionService.cacheFormDef(
                ArgumentMatchers.any(
                    FormSession::class.java
                )
            )
        ).thenCallRealMethod()
    }
}
