package engine;

import exceptions.ApplicationConfigException;
import exceptions.FormattedApplicationConfigException;
import installers.FormplayerInstallerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.modern.reference.JavaHttpRoot;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.io.BufferedInputStream;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.ResourceReferenceFactory;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Created by willpride on 11/22/16.
 */
public class FormplayerConfigEngine extends CommCareConfigEngine {

    private final Log log = LogFactory.getLog(FormplayerConfigEngine.class);

    public FormplayerConfigEngine(IStorageIndexedFactory storageFactory,
                                  FormplayerInstallerFactory formplayerInstallerFactory,
                                  ArchiveFileRoot formplayerArchiveFileRoot) {
        super(storageFactory, formplayerInstallerFactory, System.out);
        this.mArchiveRoot = formplayerArchiveFileRoot;
        ReferenceManager.instance().addReferenceFactory(formplayerArchiveFileRoot);
    }
    
    private String parseAppId(String url) {
        String appId = null;
        try {
            List<NameValuePair> params = new URIBuilder(url).getQueryParams();
            for (NameValuePair param: params) {
                if (param.getName().equals("app_id")) {
                    appId = param.getValue();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return appId;
    }

    @Override
    public void initFromArchive(String archiveURL) throws InstallCancelledException,
            UnresolvedResourceException, UnfullfilledRequirementsException {
        initFromArchive(archiveURL, false);
    }

    public void initFromArchive(String archiveURL, boolean preview) throws InstallCancelledException,
            UnresolvedResourceException, UnfullfilledRequirementsException {
        String fileName;
        String appId = null;
        if (archiveURL.startsWith("http")) {
            appId = parseAppId(archiveURL);
            if (!preview) {
                try {
                    mArchiveRoot.derive("jr://archive/" + appId + "/");
                    init("jr://archive/" + appId + "/profile.ccpr");
                    log.info(String.format("Successfully re-used installation CCZ for appId %s", appId));
                    return;
                } catch (InvalidReferenceException e) {
                    // Expected in many cases, pass
                }
            }
            fileName = downloadToTemp(archiveURL);
        } else {
            fileName = archiveURL;
        }
        ZipFile zip;
        try {
            zip = new ZipFile(fileName);
        } catch (IOException e) {
            log.error("File at " + archiveURL + ": is not a valid CommCare Package. Downloaded to: " + fileName, e);
            return;
        }
        String archiveGUID = this.mArchiveRoot.addArchiveFile(zip, appId);
        init("jr://archive/" + archiveGUID + "/profile.ccpr");
    }

    @Override
    protected String downloadToTemp(String resource) {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        try {
            URL url = new URL(resource);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true);  //you still need to handle redirect manully.
            HttpURLConnection.setFollowRedirects(true);

            if (conn.getResponseCode() == 400) {
                handleInstallError(conn.getErrorStream());
            } else if (conn.getResponseCode() == 503) {
                throw new RuntimeException(
                        "Server is too busy. Please try again in a moment."
                );
            } else if (conn.getResponseCode() == 500) {
                throw new ApplicationConfigException(
                        "There are errors in your application. Please fix these errors in your application before using app preview."
                );
            } else if (conn.getResponseCode() == 504) {
                throw new RuntimeException(
                        "Timed out fetching the CommCare application. Please submit a ticket if you continue to see this."
                );
            } else if (conn.getResponseCode() >= 400) {
                throw new RuntimeException(
                        "Formplayer encountered an unknown error. Please submit a ticket if you continue to see this."
                );
            }
            InputStream result = conn.getInputStream();

            File file = File.createTempFile("commcare_", ".ccz");

            fos = new FileOutputStream(file);
            bis = new BufferedInputStream(result);
            StreamsUtil.writeFromInputToOutput(bis, fos);
            return file.getAbsolutePath();
        } catch (IOException e) {
            log.error("Issue downloading or create stream for " + resource);
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                log.error("Exception closing output stream " + fos, e);
            }
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                log.error("Exception closing input stream " + bis, e);
            }
        }
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

    @Override
    protected void setRoots() {
        ReferenceManager.instance().addReferenceFactory(new JavaHttpRoot());
        ReferenceManager.instance().addReferenceFactory(new ResourceReferenceFactory());
        ReferenceManager.instance().addReferenceFactory(new ClasspathFileRoot());
    }

    @Override
    public void initEnvironment() throws ResourceInitializationException {
        super.initEnvironment();
        Localization.registerLanguageReference("default",
                "jr://springfile/formplayer_translatable_strings.txt");
    }
}
