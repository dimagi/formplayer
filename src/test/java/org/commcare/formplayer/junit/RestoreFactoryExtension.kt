package org.commcare.formplayer.junit

import org.commcare.formplayer.auth.DjangoAuth
import org.commcare.formplayer.services.RestoreFactory
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.invocation.InvocationOnMock
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import kotlin.collections.HashSet

/**
 * Junit extension to configure the restore factory
 *
 * This extension can be registered programmatically as described in the Junit documentation:
 * https://junit.org/junit5/docs/current/user-guide/#extensions-registration-programmatic.
 *
 * Usage example:
 * <pre>
 * class ExampleTest {
 *
 *   @RegisterExtension
 *   static RestoreFactoryExtension restoreExt = RestoreFactoryExtension.builder()
 *       .withUser("username").withDomain("test").build();
 * }
 * </pre>
 */
class RestoreFactoryExtension(
    private val username: String,
    private val domain: String,
    private val asUser: String?,
    var restorePath: String = "test_restore.xml") : BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

    lateinit var restoreFactory: RestoreFactory
    private val sessionSelectionsCache: MutableSet<String> = HashSet()

    class builder @JvmOverloads constructor(
        private var username: String = "username",
        private var domain: String = "domain",
        private var asUser: String? = null,
        private var restorePath: String = "test_restore.xml",
    ) {
        fun withUser(username: String) = apply {this.username = username}
        fun withDomain(domain: String) = apply {this.domain = domain}
        fun withAsUser(asUser: String) = apply {this.asUser = asUser}
        fun withRestorePath(restorePath: String) = apply {this.restorePath = restorePath}

        fun build(): RestoreFactoryExtension {
            return RestoreFactoryExtension(
                username, domain, asUser, restorePath
            )
        }
    }

    override fun beforeAll(context: ExtensionContext) {
        restoreFactory = SpringExtension.getApplicationContext(context).getBean(RestoreFactory::class.java)
    }

    override fun beforeEach(context: ExtensionContext) {
        reset()
        restoreFactory.configure(username, domain, asUser, DjangoAuth("test"))
        configureMock()
    }

    override fun afterEach(context: ExtensionContext) {
        reset()
    }

    fun reset() {
        sessionSelectionsCache.clear()
        restoreFactory.sqLiteDB.closeConnection()
    }

    fun configureMock() {
        mockGetRestoreXml()
        mockIsRestoreXmlExpired()
        mockCacheSessionSelections()
        mockIsConfirmedSelections()
    }

    private fun mockIsConfirmedSelections() {
        doAnswer { invocation: InvocationOnMock ->
            val selections = invocation.arguments[0] as Array<String>
            sessionSelectionsCache.contains(java.lang.String.join("|", *selections))
        }.`when`(restoreFactory).isConfirmedSelection(
            ArgumentMatchers.any(
                Array<String>::class.java
            )
        )
    }

    private fun mockCacheSessionSelections() {
        doAnswer { invocation: InvocationOnMock ->
            val selections = invocation.arguments[0] as Array<String>
            sessionSelectionsCache.add(java.lang.String.join("|", *selections))
            null
        }.`when`(restoreFactory).cacheSessionSelections(
            ArgumentMatchers.any(
                Array<String>::class.java
            )
        )
    }

    private fun mockIsRestoreXmlExpired() {
        Mockito.doReturn(false).`when`(restoreFactory).isRestoreXmlExpired
    }

    private fun mockGetRestoreXml() {
        val answer = RestoreFactoryAnswer(restorePath)
        doAnswer(answer).`when`(restoreFactory).getRestoreXml(anyBoolean())
    }
}
