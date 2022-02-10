package org.commcare.formplayer.engine;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.commcare.formplayer.exceptions.ApplicationConfigException;
import org.commcare.formplayer.exceptions.FormattedApplicationConfigException;
import org.commcare.formplayer.installers.FormplayerInstallerFactory;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.commcare.modern.reference.JavaHttpRoot;
import org.commcare.resources.model.InstallCancelledException;
import org.commcare.resources.model.ResourceInitializationException;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.util.engine.CommCareConfigEngine;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManager;
import org.javarosa.core.reference.ResourceReferenceFactory;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.storage.IStorageIndexedFactory;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipFile;

/**
 * Created by willpride on 11/22/16.
 */
public class FormplayerConfigEngine extends CommCareConfigEngine {

    private final Log log = LogFactory.getLog(FormplayerConfigEngine.class);
    private RestTemplate restTemplate;

    public FormplayerConfigEngine(IStorageIndexedFactory storageFactory,
                                  FormplayerInstallerFactory formplayerInstallerFactory,
                                  ArchiveFileRoot formplayerArchiveFileRoot,
                                  RestTemplate restTemplate) {
        super(storageFactory, formplayerInstallerFactory, System.out);
        this.restTemplate = restTemplate;
        this.mArchiveRoot = formplayerArchiveFileRoot;
        ReferenceManager.instance().addReferenceFactory(formplayerArchiveFileRoot);
    }

    private String parseAppId(String url) {
        String appId = null;
        try {
            List<NameValuePair> params = new URIBuilder(url).getQueryParams();
            for (NameValuePair param : params) {
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
        File file = null;
        try {
            file = restTemplate.execute(resource, HttpMethod.GET, null, clientHttpResponse -> {
                File ret = File.createTempFile("commcare_", ".ccz");
                StreamUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(ret));
                return ret;
            });
        } catch (HttpClientErrorException.BadRequest e) {
            handleInstallError(e.getResponseBodyAsString());
        } catch (HttpServerErrorException.ServiceUnavailable e) {
            throw new RuntimeException(
                    "Server is too busy. Please try again in a moment."
            );
        } catch (HttpClientErrorException e) {
            throw new RuntimeException(
                    "Formplayer encountered an unknown error. Please submit a ticket if you continue to see this."
            );
        } catch (HttpServerErrorException.GatewayTimeout e) {
            throw new RuntimeException(
                    "Timed out fetching the CommCare application. Please submit a ticket if you continue to see this."
            );
        } catch (HttpServerErrorException.InternalServerError e) {
            String errorMessage = parseErrorFromResponse(e.getResponseBodyAsString());
            if (StringUtils.isEmpty(errorMessage)) {
                errorMessage = "There are errors in your application. Please fix these errors in your application before using app preview.";
            }
            throw new ApplicationConfigException(errorMessage);
        }
        return Objects.requireNonNull(file).getAbsolutePath();
    }

    private String parseErrorFromResponse(String responseBody) {
        try {
            JSONObject errorJson = new JSONObject(responseBody);
            if (errorJson.has("errors")) {
                JSONArray errors = errorJson.getJSONArray("errors");
                String consolidatedErrorMessage = "";
                for (int i = 0; i < errors.length(); i++) {
                    consolidatedErrorMessage += errors.getString(i);
                    consolidatedErrorMessage += "\n";
                }
                return consolidatedErrorMessage;
            }
        } catch (JSONException e) {
            return null;
        }
        return null;
    }

    private void handleInstallError(String responseBody) {
        JSONObject errorJson = new JSONObject(responseBody);
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
