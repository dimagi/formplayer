package org.commcare.formplayer.junit

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.commcare.formplayer.auth.DjangoAuth
import org.commcare.formplayer.beans.InstallRequestBean
import org.commcare.formplayer.beans.menus.CommandListResponseBean
import org.commcare.formplayer.services.FormplayerStorageFactory
import org.commcare.formplayer.services.MenuSessionFactory
import org.commcare.formplayer.services.MenuSessionRunnerService
import org.commcare.formplayer.services.RestoreFactory
import org.commcare.formplayer.session.MenuSession
import org.commcare.formplayer.util.SessionUtils
import org.commcare.formplayer.utils.CheckedSupplier
import org.commcare.formplayer.utils.FileUtils
import org.commcare.modern.util.Pair
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import java.io.IOException
import java.io.StringReader

/**
 * Utility for performing app installs in tests.
 */
class Installer(
    private val restoreFactory: RestoreFactory,
    private val storageFactory: FormplayerStorageFactory,
    private val menuSessionFactory: MenuSessionFactory,
    private val menuSessionRunnerService: MenuSessionRunnerService
) {

    /**
     * This function performs an app install outside of the request cycle. In order to do that
     * successfully it mimics behaviour in the UserRestoreAspect and the AppInstallAspect.
     *
     * @param requestPath path to JSON file with InstallRequestBean content as well as
     * 'installReference'
     */
    @Throws(Exception::class)
    fun doInstall(requestPath: String): CommandListResponseBean? {
        val refAndBean = getInstallReferenceAndBean(
            requestPath,
            InstallRequestBean::class.java
        )
        val bean = refAndBean.second
        storageFactory.configure(bean)
        restoreFactory.configure(bean, DjangoAuth("key"))
        if (bean.isMustRestore) {
            restoreFactory.performTimedSync()
        }
        val install = CheckedSupplier {
            val menuSession: MenuSession = menuSessionFactory.buildSession(
                bean.username,
                bean.domain,
                bean.appId,
                bean.locale,
                bean.oneQuestionPerScreen,
                bean.restoreAs,
                bean.preview
            )
            menuSessionRunnerService.getNextMenu(menuSession) as CommandListResponseBean
        }
        return mockInstallReference(install, refAndBean.first)
    }

    companion object {
        private val mapper = ObjectMapper()

        /**
         * Utility method to extract the 'installReference' field from test JSON files and map the
         * remaining JSON fields to the given bean type.
         *
         * @param requestPath classpath relative path to JSON file
         * @param clazz       Bean class map JSON to
         * @return Pair of 'installReference' and deserialized JSON bean
         * @throws IOException
         */
        @JvmStatic
        @Throws(IOException::class)
        fun <T> getInstallReferenceAndBean(requestPath: String, clazz: Class<T>): Pair<String, T> {
            val root: JsonNode = mapper.readTree(
                StringReader(FileUtils.getFile(this.javaClass, requestPath))
            )
            val installReference = (root as ObjectNode).remove("installReference").asText()
            val beanContent: String = mapper.writeValueAsString(root)
            val bean = mapper.readValue(beanContent, clazz)
            return Pair(installReference, bean)
        }

        @JvmStatic
        @Throws(Exception::class)
        fun <T> mockInstallReference(supplier: CheckedSupplier<T>, installReference: String): T {
            val answer: Answer<*> = Answer { invocation: InvocationOnMock ->
                val method = invocation.method
                // only mock the `resolveInstallReference` method
                if ("resolveInstallReference" == method.name) {
                    return@Answer installReference
                } else {
                    return@Answer invocation.callRealMethod()
                }
            }
            Mockito.mockStatic(SessionUtils::class.java, answer).use { _ ->
                return supplier.get()
            }
        }
    }
}
