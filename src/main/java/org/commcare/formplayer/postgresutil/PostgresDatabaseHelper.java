package org.commcare.formplayer.postgresutil;

import org.commcare.modern.database.TableBuilder;
import org.commcare.modern.models.EncryptedModel;
import org.commcare.modern.util.Pair;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import org.javarosa.core.util.externalizable.Externalizable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author $|-|!˅@M
 */
public class PostgresDatabaseHelper {

    public static String getTableCreateString(String storageKey, Persistable p) {
        PostgresTableBuilder builder = new PostgresTableBuilder(storageKey);
        builder.addData(p);
        return builder.getTableCreateString();
    }

    public static Pair<String, List<Object>> getTableInsertData(String storageKey,
                                                                Persistable p) {
        PostgresTableBuilder builder = new PostgresTableBuilder(storageKey);
        builder.addData(p);
        return builder.getTableInsertData(p);
    }
}
