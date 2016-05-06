/**
 *
 */
package install;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.api.persistence.SqliteIndexedStorageUtility;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.modern.reference.JavaFileRoot;
import org.commcare.modern.reference.JavaHttpRoot;
import org.commcare.modern.reference.JavaResourceRoot;
import org.commcare.resources.ResourceManager;
import org.commcare.resources.model.*;
import org.commcare.resources.model.installers.LocaleFileInstaller;
import org.commcare.suite.model.*;
import org.commcare.util.CommCarePlatform;
import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.services.PropertyManager;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.*;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathMissingInstanceException;
import util.PrototypeUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipFile;


/**
 * @author ctsims
 *
 */
public class FormplayerConfigEngine {
    private ResourceTable table;
    private ResourceTable updateTable;
    private ResourceTable recoveryTable;
    private CommCarePlatform platform;
    private int fileuricount = 0;
    private ArchiveFileRoot mArchiveRoot;
    Log log = LogFactory.getLog(FormplayerConfigEngine.class);

    public FormplayerConfigEngine(final String username, final String dbPath) {
        this.platform = new CommCarePlatform(2, 27);
        log.info("FormplayerConfigEngine for username: " + username + " with dbPath: " + dbPath);

        File dbFolder = new File(dbPath);
        if(!dbFolder.exists()){
            dbFolder.mkdirs();
        } else{
            dbFolder.delete();
            dbFolder.mkdirs();
        }

        PrototypeUtils.setupPrototypes();

        setRoots();

        table = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility<>(Resource.class,
                "GLOBAL_RESOURCE_TABLE", username, dbPath));
        updateTable = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility<>(Resource.class,
                "UPDATE_RESOURCE_TABLE", username, dbPath));
        recoveryTable = ResourceTable.RetrieveTable(new SqliteIndexedStorageUtility<>(Resource.class,
                "RECOVERY_RESOURCE_TABLE", username, dbPath));


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
        // Need to do this to register the propertymanager with the new storage (ugh)
        PropertyManager.initDefaultPropertyManager();

        StorageManager.registerStorage(Profile.STORAGE_KEY, Profile.class);
        StorageManager.registerStorage(Suite.STORAGE_KEY, Suite.class);
        StorageManager.registerStorage(FormDef.STORAGE_KEY,FormDef.class);
        StorageManager.registerStorage(FormInstance.STORAGE_KEY, FormInstance.class);
    }

    private void setRoots() {
        ReferenceManager._().addReferenceFactory(new JavaHttpRoot());

        this.mArchiveRoot = new ArchiveFileRoot();

        ReferenceManager._().addReferenceFactory(mArchiveRoot);

        ReferenceManager._().addReferenceFactory(new JavaResourceRoot(this.getClass()));
    }

    public void initFromArchive(String archiveURL) throws IOException, InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException {
        String fileName;
        if(archiveURL.startsWith("http")) {
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
        try{
            URL url = new URL(resource);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);  //you still need to handle redirect manully.
            HttpURLConnection.setFollowRedirects(true);

            File file = File.createTempFile("commcare_", ".ccz");

            FileOutputStream fos = new FileOutputStream(file);
            StreamsUtil.writeFromInputToOutput(new BufferedInputStream(conn.getInputStream()), fos);
            return file.getAbsolutePath();
        } catch(IOException e) {
            log.error("Issue downloading or create stream for " +resource);
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    public void initFromLocalFileResource(String resource) throws InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException {
        String reference = setFileSystemRootFromResourceAndReturnRelativeRef(resource);
        init(reference);
    }

    private String setFileSystemRootFromResourceAndReturnRelativeRef(String resource) {
        int lastSeparator = resource.lastIndexOf(File.separator);

        String rootPath;
        String filePart;

        if(lastSeparator == -1 ) {
            rootPath = new File("").getAbsolutePath();
            filePart = resource;
        } else {
            //Get the location of the file. In the future, we'll treat this as the resource root
            rootPath = resource.substring(0,resource.lastIndexOf(File.separator));

            //cut off the end
            filePart = resource.substring(resource.lastIndexOf(File.separator) + 1);
        }

        //(That root now reads as jr://file/)
        ReferenceManager._().addReferenceFactory(new JavaFileRoot(rootPath));

        //Now build the testing reference we'll use
        System.out.println("Returning file root: " + "jr://file/" + filePart);
        return "jr://file/" + filePart;
    }

    /**
     * super, super hacky for now, gets a jar directory and loads language resources
     * from it.
     * @param pathToResources
     */
    public void addJarResources(String pathToResources) {
        File resources = new File(pathToResources);
        if(!resources.exists() && resources.isDirectory()) {
            throw new RuntimeException("Couldn't find jar resources at " + resources.getAbsolutePath() + " . Please correct the path, or use the -nojarresources flag to skip loading jar resources.");
        }

        fileuricount++;
        String jrroot = "extfile" + fileuricount;
        ReferenceManager._().addReferenceFactory(new JavaFileRoot(new String[]{jrroot}, resources.getAbsolutePath()));

        for(File file : resources.listFiles()) {
            String name = file.getName();
            if(name.endsWith("txt")) {
                ResourceLocation location = new ResourceLocation(Resource.RESOURCE_AUTHORITY_LOCAL, "jr://" + jrroot + "/" + name);
                Vector<ResourceLocation> locations = new Vector<ResourceLocation>();
                locations.add(location);
                if(!(name.lastIndexOf("_") < name.lastIndexOf("."))) {
                    //skip it
                } else {
                    String locale = name.substring(name.lastIndexOf("_") + 1, name.lastIndexOf("."));
                    Resource test = new Resource(-2, name, locations, "Internal Strings: " + locale);
                    try {
                        table.addResource(test, new LocaleFileInstaller(locale),null);
                    } catch (StorageFullException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } else {
                //we don't support other file types yet
            }
        }
    }

    private void init(String profileRef) throws InstallCancelledException, UnresolvedResourceException, UnfullfilledRequirementsException {
        installAppFromReference(profileRef);
    }

    public void installAppFromReference(String profileReference) throws UnresolvedResourceException,
            UnfullfilledRequirementsException, InstallCancelledException {
        ResourceManager.installAppResources(platform, profileReference, this.table, true);
    }

    public void initEnvironment() {
        try {
            Localization.init(true);
            table.initializeResources(platform);
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
            log.error("Error while initializing one of the resolved resources");
            System.exit(-1);
        }
    }

    public CommCarePlatform getPlatform() {
        return platform;
    }

    public FormDef loadFormByXmlns(String xmlns) {
        IStorageUtilityIndexed<FormDef> formStorage =
                (IStorageUtilityIndexed)StorageManager.getStorage(FormDef.STORAGE_KEY);
        return formStorage.getRecordForValue("XMLNS", xmlns);
    }

    final static private class QuickStateListener implements TableStateListener{
        int lastComplete = 0;

        @Override
        public void resourceStateUpdated(ResourceTable table) {

        }

        @Override
        public void incrementProgress(int complete, int total) {
            int diff = complete - lastComplete;
            lastComplete = complete;
            for(int i = 0 ; i < diff ; ++i) {
                System.out.print(".");
            }
        }
    };

    public void attemptAppUpdate(boolean forceNew) {
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
            if (forceNew &&
                    ("https".equals(authUrl.getProtocol()) ||
                            "http".equals(authUrl.getProtocol()))) {
                if (authUrl.getQuery() != null) {
                    // If the profileRef url already have query strings
                    // just add a new one to the end
                    authRef = authRef + "&target=build";
                } else {
                    // otherwise, start off the query string with a ?
                    authRef = authRef + "?target=build";
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
            System.out.print("Installing update");

            // Replaces global table with temporary, or w/ recovery if
            // something goes wrong
            resourceManager.upgrade();
        } catch(UnresolvedResourceException e) {
            System.out.println("Update Failed! Couldn't find or install one of the remote resources");
            e.printStackTrace();
            return;
        } catch(UnfullfilledRequirementsException e) {
            System.out.println("Update Failed! This CLI host is incompatible with the app");
            e.printStackTrace();
            return;
        } catch(Exception e) {
            System.out.println("Update Failed! There is a problem with one of the resources");
            e.printStackTrace();
            return;
        }

        // Initializes app resources and the app itself, including doing a check to see if this
        // app record was converted by the db upgrader
        initEnvironment();
    }
}
