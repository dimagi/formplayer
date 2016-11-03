package services;

import auth.HqAuth;
import beans.NewFormResponse;
import beans.NewSessionRequestBean;
import hq.CaseAPIs;
import objects.SerializableFormSession;
import org.commcare.api.persistence.UserSqlSandbox;
import org.javarosa.core.model.FormDef;
import org.javarosa.xform.parse.XFormParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import repo.FormSessionRepo;
import session.FormSession;
import util.StringUtils;

import java.io.IOException;
import java.io.StringReader;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 */
@Component
public class FormSessionFactory {

    @Value("${commcarehq.host}")
    private String host;

    @Autowired
    private RestoreFactory restoreFactory;

    public FormSession getFormSession(SerializableFormSession serializableFormSession) throws Exception {
        if (serializableFormSession.getRestoreXml() == null) {
            serializableFormSession.setRestoreXml(restoreFactory.getRestoreXml());
        }
        if(serializableFormSession.getPostUrl() == null) {
            serializableFormSession.setPostUrl(
                    StringUtils.getPostUrl(host,
                            serializableFormSession.getDomain(),
                            serializableFormSession.getAppId()));
        }
        return new FormSession(serializableFormSession);
    }
}
