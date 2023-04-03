package org.commcare.formplayer.sandbox

import org.commcare.cases.model.Case
import org.commcare.formplayer.services.ConnectionHandler

class CaseSearchSqlSandbox(searchKey: String, handler: ConnectionHandler) : UserSqlSandbox(handler, false), ConnectionHandler {

    private val tableName : String = FORMPLAYER_CASE + searchKey
    private val caseSearchStorage = SqlStorage(handler, Case::class.java, tableName, false)

    override fun getCaseStorage(): SqlStorage<Case> {
       return caseSearchStorage
    }
}
