package org.commcare.formplayer.db.migration;

import java.util.Arrays;

/**
 * Changes the Current Index field to be VARCHAR instead of INTEGER
 */
public class V7__formsessions_alter_current_index extends BaseFormplayerMigration {
  @Override
  public Iterable<String> getSqlStatements() {
    return Arrays.asList(
            "ALTER TABLE formplayer_sessions " +
        "ADD COLUMN temp_currentIndex text",

        "UPDATE formplayer_sessions " +
        "SET temp_currentIndex = cast(cast(currentIndex as varchar) as text)",

        "ALTER TABLE formplayer_sessions DROP COLUMN currentIndex",

        "ALTER TABLE formplayer_sessions ADD COLUMN currentIndex text DEFAULT '-1'",

        "UPDATE formplayer_sessions SET currentIndex = temp_currentIndex",

        "ALTER TABLE formplayer_sessions DROP COLUMN temp_currentIndex"
    );
  }
}
