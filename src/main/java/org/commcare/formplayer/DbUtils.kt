package org.commcare.formplayer

import org.commcare.formplayer.application.SQLiteProperties
import org.commcare.modern.database.TableBuilder
import java.io.File

/**
 * Utility functions required to construct Sqlite Dbs
 */
object DbUtils {

    @JvmStatic
    fun getDbPathForUser(domain: String, username: String, asUsername: String?): String {
        return SQLiteProperties.getDataDir() + domain + File.separator + TableBuilder.scrubName(
            getUsernameDetail(username, asUsername)
        )
    }


    private fun getUsernameDetail(username: String, asUsername: String?): String {
        return if (asUsername != null) {
            username + "_" + asUsername
        } else username
    }
}
