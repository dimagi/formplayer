package org.commcare.formplayer.junit

import org.commcare.formplayer.services.FormplayerStorageFactory
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.junit.jupiter.SpringExtension

class StorageFactoryExtension(
    private val username: String,
    private val domain: String,
    private val appId: String,
    private val asUser: String?,
    private val asCaseId: String?) : BeforeAllCallback, BeforeEachCallback {

    private lateinit var storageFactory: FormplayerStorageFactory

    class builder @JvmOverloads constructor(
        private var username: String = "username",
        private var domain: String = "domain",
        private var appId: String = "123",
        private var asUser: String? = null,
        private var asCaseId: String? = null,
    ) {
        fun withUser(username: String) = apply {this.username = username}
        fun withDomain(domain: String) = apply {this.domain = domain}
        fun withAppId(appId: String) = apply {this.appId = appId}
        fun withAsUser(asUser: String) = apply {this.asUser = asUser}
        fun withCase(asCaseId: String) = apply {this.asCaseId = asCaseId}
        fun build(): StorageFactoryExtension {
            return StorageFactoryExtension(
                username, domain, appId, asUser, asCaseId
            )
        }
    }


    override fun beforeAll(context: ExtensionContext) {
        storageFactory = SpringExtension.getApplicationContext(context).getBean(FormplayerStorageFactory::class.java)
    }

    override fun beforeEach(context: ExtensionContext?) {
        storageFactory.configure(username, domain, appId, asUser, asCaseId)
    }
}
