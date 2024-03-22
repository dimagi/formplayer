package org.commcare.formplayer.sqlitedb

class CaseSearchDB(domain: String, username: String, asUserName: String?) : SQLiteDB(
    CaseSearchDbPath(
        domain,
        username,
        asUserName
    )
)
