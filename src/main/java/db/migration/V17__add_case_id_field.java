package db.migration;

import java.util.Arrays;

/**
 * Created by willpride on 11/9/17.
 */
public class V17__add_case_id_field extends BaseFormplayerMigration {
    @Override
    public Iterable<String> getSqlStatements() {
        return Arrays.asList("ALTER TABLE formplayer_sessions " +
                "ADD caseId VARCHAR DEFAULT NULL");
    }
}
