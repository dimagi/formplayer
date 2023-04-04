package org.commcare.formplayer.sandbox

import org.commcare.cases.model.Case
import org.commcare.formplayer.services.ConnectionHandler

class CaseSearchSqlSandbox(tableName: String, handler: ConnectionHandler) : UserSqlSandbox(handler, false), ConnectionHandler {

    private val caseSearchStorage = SqlStorage(handler, Case::class.java, tableName, false)

    override fun getCaseStorage(): SqlStorage<Case> {
       return caseSearchStorage
    }
}
