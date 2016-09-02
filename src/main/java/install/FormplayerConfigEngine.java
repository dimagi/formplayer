/**
 *
 */
package install;

import exceptions.ApplicationConfigException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.modern.reference.JavaFileRoot;
import org.commcare.modern.reference.JavaHttpRoot;
import org.commcare.resources.ResourceManager;
import org.commcare.resources.model.*;
import org.commcare.suite.model.Profile;
import org.commcare.suite.model.Suite;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageFactory;
import org.javarosa.core.services.storage.IStorageUtility;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.javarosa.core.services.storage.StorageManager;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.json.JSONObject;
import util.PrototypeUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipFile;


/**
 * @author wspride
 */
public class FormplayerConfigEngine {
    private final ResourceTable table;
    private final CommCarePlatform platform;
    private ArchiveFileRoot mArchiveRoot;
    private final Log log = LogFactory.getLog(FormplayerConfigEngine.class);

    public FormplayerConfigEngine(final String username, final String dbPath) {
        this.platform = new CommCarePlatform(2, 27);
        log.info("FormplayerConfigEngine for username: " + username + " with dbPath: " + dbPath);

        File dbFolder = new File(dbPath);
        if (!dbFolder.exists()) {
            dbFolder.mkdirs();
        } else{
            dbFolder.delete();
            dbFolder.mkdirs();
        }

        PrototypeUtils.setupPrototypes();

        setRoots();

        table = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility<>(Resource.class,
                "GLOBAL_RESOURCE_TABLE", username, dbPath));
        ResourceTable updateTable = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility<>(Resource.class,
                "UPDATE_RESOURCE_TABLE", username, dbPath));
        ResourceTable recoveryTable = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility<>(Resource.class,
                "RECOVERY_RESOURCE_TABLE", username, dbPath));

        setupStorageManager(username, dbPath);
    }

    public static void setupStorageManager(final String username, final String dbPath) {
        //All of the below is on account of the fact that the installers
        //aren't going through a factory method to handle them differently
        //per device.
        StorageManager.forceClear();
        StorageManager.setStorageFactory(new IStorageFactory() {
            public IStorageUtility newStorage(String name, Class type) {
                return new SqliteIndexedStorageUtility<>(type,
                        name, username, dbPath);
            }

        });
        // Need to do this to register the PropertyManager with the new storage (ugh)
        PropertyManager.initDefaultPropertyManager();
        System.out.println("Registered Property Manager for username: " + username);
        StorageManager.registerStorage(Profile.STORAGE_KEY, Profile.class);
        StorageManager.registerStorage(Suite.STORAGE_KEY, Suite.class);
        StorageManager.registerStorage(FormDef.STORAGE_KEY, FormDef.class);
        StorageManager.registerStorage(FormInstance.STORAGE_KEY, FormInstance.class);
    }

    private void setRoots() {
        ReferenceManager._().addReferenceFactory(new JavaHttpRoot());

        this.mArchiveRoot = new ArchiveFileRoot();

        ReferenceManager._().addReferenceFactory(mArchiveRoot);
    }

    public void initFromArchive(String archiveURL) throws IOException, InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException {
        String fileName;
        if (archiveURL.startsWith("http")) {
            fileName = downloadToTemp(archiveURL);
        } else {
            fileName = archiveURL;
        }
        ZipFile zip;
        try {
            zip = new ZipFile(fileName);
        } catch (IOException e) {
            log.error("File at " + archiveURL + ": is not a valid CommCare Package. Downloaded to: " + fileName);
            e.printStackTrace();
            throw e;
        }
        String archiveGUID = this.mArchiveRoot.addArchiveFile(zip);

        init("jr://archive/" + archiveGUID + "/profile.ccpr");
    }

    private String downloadToTemp(String resource) {
        try {
            URL url = new URL(resource);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);  //you still need to handle redirect manully.
            HttpURLConnection.setFollowRedirects(true);
            if (conn.getResponseCode() == 400) {
                handleInstallError(conn.getErrorStream());
            }
            InputStream result = conn.getInputStream();

            File file = File.createTempFile("commcare_", ".ccz");

            FileOutputStream fos = new FileOutputStream(file);
            StreamsUtil.writeFromInputToOutput(new BufferedInputStream(result), fos);
            return file.getAbsolutePath();
        } catch (IOException e) {
            log.error("Issue downloading or create stream for " + resource);
            e.printStackTrace();
            return null;
        }
    }

    public void initFromLocalFileResource(String resource) throws InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException {
        String reference = setFileSystemRootFromResourceAndReturnRelativeRef(resource);
        init(reference);
    }

    private void handleInstallError(InputStream errorStream) {
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(errorStream, writer, "utf-8");
        } catch (IOException e) {
            log.error("Unable to read error stream", e);
        }
        String errorMessage = writer.toString();
        JSONObject errorJson = new JSONObject(errorMessage);
        throw new ApplicationConfigException(errorJson.getJSONArray("errors").join(" "));
    }

    private String setFileSystemRootFromResourceAndReturnRelativeRef(String resource) {
        int lastSeparator = resource.lastIndexOf(File.separator);

        String rootPath;
        String filePart;

        if (lastSeparator == -1) {
            rootPath = new File("").getAbsolutePath();
            filePart = resource;
        } else {
            //Get the location of the file. In the future, we'll treat this as the resource root
            rootPath = resource.substring(0, resource.lastIndexOf(File.separator));

            //cut off the end
            filePart = resource.substring(resource.lastIndexOf(File.separator) + 1);
        }

        //(That root now reads as jr://file/)
        ReferenceManager._().addReferenceFactory(new JavaFileRoot(rootPath));

        //Now build the testing reference we'll use
        return "jr://file/" + filePart;
    }

    private void init(String profileRef) throws InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException {
        installAppFromReference(profileRef);
    }

    private void installAppFromReference(String profileReference) throws UnresolvedResourceException,
            UnfullfilledRequirementsException, InstallCancelledException {
        ResourceManager.installAppResources(platform, profileReference, this.table, true);
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
        } catch (ResourceInitializationException e) {
            log.error("Error while initializing one of the resolved resources", e);
        }
    }

    public CommCarePlatform getPlatform() {
        return platform;
    }

    public FormDef loadFormByXmlns(String xmlns) {
        IStorageUtilityIndexed<FormDef> formStorage =
                (IStorageUtilityIndexed) StorageManager.getStorage(FormDef.STORAGE_KEY);
        return formStorage.getRecordForValue("XMLNS", xmlns);
    }
}
