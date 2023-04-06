package org.commcare.formplayer

import org.commcare.formplayer.application.SQLiteProperties
import org.commcare.formplayer.exceptions.SQLiteRuntimeException
import org.commcare.formplayer.sqlitedb.SQLiteDB
import org.commcare.modern.database.TableBuilder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.SQLException

/**
 * Utility functions required to construct Sqlite Dbs
 */
object DbUtils {

    @JvmStatic
    fun getDbPathForUser(domain: String, username: String, asUsername: String?): String {
        return SQLiteProperties.getDataDir() + getDbPathSuffix(domain, username, asUsername)
    }

    /**
     * Returns specifc db path for the given domain, username and asUserName
     * This should be combined with SQLiteProperties.getDataDir() to form the absolute path for a db
     */
    @JvmStatic
    fun getDbPathSuffix(domain: String, username: String, asUserName: String?) : String {
        return Paths.get(domain, TableBuilder.scrubName(
            getUsernameDetail(username, asUserName)
        )).toString()
    }

    private fun getUsernameDetail(username: String, asUsername: String?): String {
        return if (asUsername != null) {
            username + "_" + asUsername
        } else username
    }

    /**
     * Set auto commit on given sqlite db
     */
    @JvmStatic
    fun setAutoCommit(sqLiteDB: SQLiteDB, autoCommit: Boolean) {
        try {
            sqLiteDB.connection.autoCommit = autoCommit
        } catch (e: SQLException) {
            throw SQLiteRuntimeException(e)
        }
    }

    /**
     * Rollbacks given sqlite db
     */
    @JvmStatic
    fun rollback(sqLiteDB: SQLiteDB) {
        try {
            sqLiteDB.connection.rollback()
        } catch (e: SQLException) {
            throw SQLiteRuntimeException(e)
        }
    }

    /**
     * Commits given sqlite db
     */
    @JvmStatic
    fun commit(sqLiteDB: SQLiteDB) {
        try {
            sqLiteDB.connection.commit()
        } catch (e: SQLException) {
            throw SQLiteRuntimeException(e)
        }
    }
}
