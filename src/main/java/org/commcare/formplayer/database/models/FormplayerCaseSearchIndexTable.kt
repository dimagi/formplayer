package org.commcare.formplayer.database.models

import org.commcare.formplayer.services.ConnectionHandler

class FormplayerCaseSearchIndexTable(connectionHandler: ConnectionHandler, private val searchKey: String) :
    FormplayerCaseIndexTable(connectionHandler, false) {

    private val CASE_SEARCH_INDEX_STORAGE_TABLE_PREFIX = "case_search_index_storage_FOR_"
    override fun getTableName(): String {
        return CASE_SEARCH_INDEX_STORAGE_TABLE_PREFIX + searchKey
    }
}
