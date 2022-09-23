package org.commcare.formplayer.junit

import org.commcare.formplayer.auth.DjangoAuth
import org.commcare.formplayer.services.RestoreFactory
import org.commcare.formplayer.tests.BaseTestClass
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import kotlin.collections.HashSet

class RestoreFactoryExtension(
    private val username: String,
    private val domain: String,
    private val asUser: String?) : BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

    private lateinit var restoreFactory: RestoreFactory
    private val sessionSelectionsCache: MutableSet<String> = HashSet()

    var restorePath: String = "test_restore.xml"

    class builder @JvmOverloads constructor(
        private var username: String = "username",
        private var domain: String = "domain",
        private var asUser: String? = null,
    ) {
        fun withUser(username: String) = apply {this.username = username}
        fun withDomain(domain: String) = apply {this.domain = domain}
        fun withAsUser(asUser: String) = apply {this.asUser = asUser}
        fun build(): RestoreFactoryExtension {
            return RestoreFactoryExtension(
                username, domain, asUser
            )
        }
    }

    fun withRestore(restorePath: String) {
        val answer = BaseTestClass.RestoreFactoryAnswer(restorePath)
        Mockito.doAnswer(answer).`when`(restoreFactory)
            .getRestoreXml(ArgumentMatchers.anyBoolean())
    }

    override fun beforeAll(context: ExtensionContext?) {
        restoreFactory = SpringExtension.getApplicationContext(context).getBean(RestoreFactory::class.java)
    }

    override fun beforeEach(context: ExtensionContext?) {
        sessionSelectionsCache.clear()
        withRestore(restorePath);
        restoreFactory.configure(username, domain, asUser, DjangoAuth("test"))

        Mockito.doReturn(false)
            .`when`(restoreFactory).isRestoreXmlExpired
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val selections = invocation.arguments[0] as Array<String>
            sessionSelectionsCache.add(java.lang.String.join("|", *selections))
            null
        }.`when`(restoreFactory).cacheSessionSelections(
            ArgumentMatchers.any(
                Array<String>::class.java
            )
        )
        Mockito.doAnswer { invocation: InvocationOnMock ->
            val selections = invocation.arguments[0] as Array<String>
            sessionSelectionsCache.contains(java.lang.String.join("|", *selections))
        }.`when`(restoreFactory).isConfirmedSelection(
            ArgumentMatchers.any(
                Array<String>::class.java
            )
        )
    }

    override fun afterEach(context: ExtensionContext?) {
        sessionSelectionsCache.clear()
    }
}
