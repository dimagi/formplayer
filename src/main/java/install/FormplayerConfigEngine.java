/**
 *
 */
package install;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.resources.model.*;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.commcare.util.engine.CommCareConfigEngine;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.*;
import org.javarosa.core.services.storage.util.DummyIndexedStorageUtility;

import java.io.*;


/**
 * @author wspride
 */
public class FormplayerConfigEngine extends CommCareConfigEngine{

    private final Log log = LogFactory.getLog(FormplayerConfigEngine.class);

    public FormplayerConfigEngine(final String username, final String dbPath) {
        this.platform = new CommCarePlatform(2, 31);
        log.info("FormplayerConfigEngine for username: " + username + " with dbPath: " + dbPath);
        String trimmedUsername = StringUtils.substringBefore(username, "@");

        File dbFolder = new File(dbPath);
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();
        } else{
            dbFolder.delete();
            dbFolder.mkdirs();
        }

        setRoots();

        table = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility<>(Resource.class,
                "GLOBAL_RESOURCE_TABLE", trimmedUsername, dbPath));
        updateTable = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility<>(Resource.class,
                "UPDATE_RESOURCE_TABLE", trimmedUsername, dbPath));
        recoveryTable = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility<>(Resource.class,
                "RECOVERY_RESOURCE_TABLE", trimmedUsername, dbPath));

        setupStorageManager(trimmedUsername, dbPath);
    }

    private void setupStorageManager(final String trimmedUsername, final String dbPath) {
        //All of the below is on account of the fact that the installers
        //aren't going through a factory method to handle them differently
        //per device.
        StorageManager.forceClear();
        StorageManager.setStorageFactory(new IStorageFactory() {
            public IStorageUtility newStorage(String name, Class type) {
                return new SqliteIndexedStorageUtility<>(type,
                        name, trimmedUsername, dbPath);
            }

        });
        // Need to do this to register the PropertyManager with the new storage (ugh)
        PropertyManager.initDefaultPropertyManager();
        System.out.println("Registered Property Manager for username: " + trimmedUsername);
        StorageManager.registerStorage(Profile.STORAGE_KEY, Profile.class);
        StorageManager.registerStorage(Suite.STORAGE_KEY, Suite.class);
        StorageManager.registerStorage(FormDef.STORAGE_KEY, FormDef.class);
        StorageManager.registerStorage(FormInstance.STORAGE_KEY, FormInstance.class);
    }

    public void initEnvironment() {
        try {
            Localization.init(true);
            table.initializeResources(platform, false);
            //Make sure there's a default locale, since the app doesn't necessarily use the
            //localization engine
            Localization.getGlobalLocalizerAdvanced().addAvailableLocale("default");

            Localization.setDefaultLocale("default");

            String newLocale = null;
            for (String locale : Localization.getGlobalLocalizerAdvanced().getAvailableLocales()) {
                if (newLocale == null) {
                    newLocale = locale;
                }
                System.out.println("* " + locale);
            }

            Localization.setLocale(newLocale);
        } catch (InvalidResourceException e) {
            log.error("Error while initializing one of the resolved resources", e);
        }
    }
}
