package org.commcare.formplayer.postgresutil;

import org.commcare.modern.database.DatabaseHelper;
import org.commcare.modern.database.Table;
import org.commcare.modern.models.MetaField;
import org.commcare.modern.util.Pair;
import org.javarosa.core.services.storage.IMetaData;
import org.javarosa.core.services.storage.Persistable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import static org.commcare.modern.database.TableBuilder.scrubName;

/**
 * @author $|-|!˅@M
 */
public class PostgresTableBuilder {

    private final String name;
    private final Vector<String> cols;
    private final Vector<String> rawCols;
    final HashSet<String> unique = new HashSet<>();

    public PostgresTableBuilder(Class c, String name) {
        this.name = name;
        cols = new Vector<>();
        rawCols = new Vector<>();
        this.addData(c);
    }

    public PostgresTableBuilder(String name) {
        this.name = name;
        cols = new Vector<>();
        rawCols = new Vector<>();
    }

    public PostgresTableBuilder(Class c) {
        this(c, ((Table)c.getAnnotation(Table.class)).value());
    }

    public void addData(Class c) {
        cols.add(DatabaseHelper.ID_COL + " SERIAL PRIMARY KEY");
        rawCols.add(DatabaseHelper.ID_COL);

        for (Field f : c.getDeclaredFields()) {
            if (f.isAnnotationPresent(MetaField.class)) {
                MetaField mf = f.getAnnotation(MetaField.class);
                addMetaField(mf);
            }
        }

        for (Method m : c.getDeclaredMethods()) {
            if (m.isAnnotationPresent(MetaField.class)) {
                MetaField mf = m.getAnnotation(MetaField.class);
                addMetaField(mf);
            }
        }

        cols.add(DatabaseHelper.DATA_COL + " BYTEA");
        rawCols.add(DatabaseHelper.DATA_COL);
    }

    protected void addMetaField(MetaField mf) {
        String key = mf.value();
        String columnName = scrubName(key);
        rawCols.add(columnName);
        String columnDef;
        columnDef = columnName;

        //Modifiers
        if (unique.contains(columnName) || mf.unique()) {
            columnDef += " UNIQUE";
        }
        cols.add(columnDef);
    }

    public void addData(Persistable p) {
        addPersistableIdAndMeta(p);

        cols.add(DatabaseHelper.DATA_COL + " BYTEA");
        rawCols.add(DatabaseHelper.DATA_COL);
    }

    private void addPersistableIdAndMeta(Persistable p) {
        cols.add(DatabaseHelper.ID_COL + " SERIAL PRIMARY KEY");
        rawCols.add(DatabaseHelper.ID_COL);

        if (p instanceof IMetaData) {
            String[] keys = ((IMetaData)p).getMetaDataFields();
            if (keys != null) {
                for (String key : keys) {
                    String columnName = scrubName(key);
                    if (!rawCols.contains(columnName)) {
                        rawCols.add(columnName);
                        String columnDef = columnName;

                        columnDef += " TEXT";
                        //Modifiers
                        if (unique.contains(columnName)) {
                            columnDef += " UNIQUE";
                        }
                        cols.add(columnDef);
                    }
                }
            }
        }
    }

    /**
     * Build a table to store provided persistable in the filesystem.  Creates
     * filepath and encrypting key columns, along with normal metadata columns
     * from the persistable
     */
    public void addFileBackedData(Persistable p) {
        addData(p);

        cols.add(DatabaseHelper.AES_COL + " BYTEA");
        rawCols.add(DatabaseHelper.AES_COL);

        cols.add(DatabaseHelper.FILE_COL);
        rawCols.add(DatabaseHelper.FILE_COL);
    }

    public void setUnique(String columnName) {
        unique.add(scrubName(columnName));
    }

    public String getTableCreateString(String currentSchema) {
        String built = "CREATE TABLE IF NOT EXISTS " + currentSchema + "." + scrubName(name) + " (";
        for (int i = 0; i < cols.size(); ++i) {
            built += cols.elementAt(i);
            if (i < cols.size() - 1) {
                built += ", ";
            }
        }
        built += ");";
        return built;
    }

    public Pair<String, List<Object>> getTableInsertData(Persistable p, String currentSchema) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("INSERT INTO ").append(currentSchema).append(".").append(scrubName(name)).append(" (");
        HashMap<String, Object> contentValues = DatabaseHelper.getMetaFieldsAndValues(p);

        ArrayList<Object> params = new ArrayList<>();

        for (int i = 0; i < rawCols.size(); ++i) {
            if (rawCols.elementAt(i).equals(DatabaseHelper.ID_COL)) {
                continue;
            }
            stringBuilder.append(rawCols.elementAt(i));
            if (i < rawCols.size() - 1) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(") VALUES (");

        for (int i = 0; i < rawCols.size(); ++i) {
            if (rawCols.elementAt(i).equals(DatabaseHelper.ID_COL)) {
                continue;
            }
            Object currentValue = contentValues.get(rawCols.elementAt(i));
            stringBuilder.append("?");
            params.add(currentValue);
            if (i < rawCols.size() - 1) {
                stringBuilder.append(", ");
            }
        }

        stringBuilder.append(");");

        return new Pair<>(stringBuilder.toString(), params);
    }
}
