package org.commcare.formplayer.sqlitedb

import org.commcare.formplayer.DbUtils.getDbPathForUser
import org.commcare.formplayer.util.Constants

class CaseSearchDbPath(private val domain: String, private  val username: String, private val asUserName: String?) : DBPath() {

    private val CASE_SEARCH_DB_PREFIX = "tmp_case_search_"

    override fun getDatabasePath(): String {
        return getDbPathForUser(domain, username, asUserName)
    }

    override fun getDatabaseName(): String {
        return CASE_SEARCH_DB_PREFIX + Constants.CASE_SEARCH_DB_VERSION
    }
}
