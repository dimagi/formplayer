/**
 *
 */
package install;

import exceptions.ApplicationConfigException;
import exceptions.FormattedApplicationConfigException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipFile;


/**
 * @author wspride
 */
public class FormplayerConfigEngine {
    private final ResourceTable table;
    private final CommCarePlatform platform;
    private ArchiveFileRoot mArchiveRoot;

    private final ResourceTable updateTable;
    private final ResourceTable recoveryTable;

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

    private void setRoots() {
        ReferenceManager.instance().addReferenceFactory(new JavaHttpRoot());

        this.mArchiveRoot = new ArchiveFileRoot();

        ReferenceManager.instance().addReferenceFactory(mArchiveRoot);
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
            } else if (conn.getResponseCode() == 500) {
                throw new ApplicationConfigException(
                    "Encountered an error while processing the application. Please submit a ticket if you continue to see this."
                );
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
        if (errorJson.has("error_html")) {
            throw new FormattedApplicationConfigException(errorJson.getString("error_html"));
        }
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
        ReferenceManager.instance().addReferenceFactory(new JavaFileRoot(rootPath));

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
        } catch (InvalidResourceException e) {
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

    /**
     * @param updateTarget Null to request the default latest build. Otherwise a string identifying
     *                     the target of the update:
     *                     'release' - Latest released (or starred) build
     *                     'build' - Latest completed build (released or not)
     *                     'save' - Latest functional saved version of the app
     */
    public void attemptAppUpdate(String updateTarget) {
        ResourceTable global = table;

        // Ok, should figure out what the state of this bad boy is.
        Resource profileRef = global.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);

        Profile profileObj = this.getPlatform().getCurrentProfile();

        global.setStateListener(new QuickStateListener());

        updateTable.setStateListener(new QuickStateListener());

        // When profileRef points is http, add appropriate dev flags
        String authRef = profileObj.getAuthReference();

        try {
            URL authUrl = new URL(authRef);

            // profileRef couldn't be parsed as a URL, so don't worry
            // about adding dev flags to the url's query

            // If we want to be using/updating to the latest build of the
            // app (instead of latest release), add it to the query tags of
            // the profile reference
            if (updateTarget != null &&
                    ("https".equals(authUrl.getProtocol()) ||
                            "http".equals(authUrl.getProtocol()))) {
                if (authUrl.getQuery() != null) {
                    // If the profileRef url already have query strings
                    // just add a new one to the end
                    authRef = authRef + "&target=" + updateTarget;
                } else {
                    // otherwise, start off the query string with a ?
                    authRef = authRef + "?target" + updateTarget;
                }
            }
        } catch (MalformedURLException e) {
            System.out.print("Warning: Unrecognized URL format: " + authRef);
        }


        try {
            // This populates the upgrade table with resources based on
            // binary files, starting with the profile file. If the new
            // profile is not a newer version, statgeUpgradeTable doesn't
            // actually pull in all the new references

            System.out.println("Checking for updates....");
            ResourceManager resourceManager = new ResourceManager(platform, global, updateTable, recoveryTable);
            resourceManager.stageUpgradeTable(authRef, true);
            Resource newProfile = updateTable.getResourceWithId(CommCarePlatform.APP_PROFILE_RESOURCE_ID);
            if (!newProfile.isNewer(profileRef)) {
                System.out.println("Your app is up to date!");
                return;
            }

            System.out.println("Update found. New Version: " + newProfile.getVersion());
            System.out.println("Downloading / Preparing Update");
            resourceManager.prepareUpgradeResources();
            System.out.print("Installing update");

            // Replaces global table with temporary, or w/ recovery if
            // something goes wrong
            resourceManager.upgrade();
        } catch (UnresolvedResourceException e) {
            System.out.println("Update Failed! Couldn't find or install one of the remote resources");
            e.printStackTrace();
            return;
        } catch (UnfullfilledRequirementsException e) {
            System.out.println("Update Failed! This CLI host is incompatible with the app");
            e.printStackTrace();
            return;
        } catch (Exception e) {
            System.out.println("Update Failed! There is a problem with one of the resources");
            e.printStackTrace();
            return;
        }

        // Initializes app resources and the app itself, including doing a check to see if this
        // app record was converted by the db upgrader
        initEnvironment();
    }

    final static private class QuickStateListener implements TableStateListener {
        int lastComplete = 0;

        @Override
        public void simpleResourceAdded() {

        }

        @Override
        public void compoundResourceAdded(ResourceTable table) {

        }

        @Override
        public void incrementProgress(int complete, int total) {
            int diff = complete - lastComplete;
            lastComplete = complete;
            for (int i = 0; i < diff; ++i) {
                System.out.print(".");
            }
        }
    }

}
