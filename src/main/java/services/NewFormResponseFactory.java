package services;

import auth.HqAuth;
import beans.NewFormResponse;
import beans.NewSessionRequestBean;
import objects.SerializableFormSession;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;
import repo.FormSessionRepo;
import session.FormSession;

import java.io.IOException;

/**
 * Class containing logic for accepting a NewSessionRequest and services,
 * restoring the user, opening the new form, and returning the question list response.
 * TODO WSP this should probably just be a static helper class, or merged with FormSession
 */
@Component
public class NewFormResponseFactory {

    private final XFormService xFormService;
    private final RestoreService restoreService;
    private final FormSessionRepo formSessionRepo;

    public NewFormResponseFactory(FormSessionRepo formSessionRepo,
                                  XFormService xFormService,
                                  RestoreService restoreService) {
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.formSessionRepo = formSessionRepo;
    }

    public NewFormResponse getResponse(NewSessionRequestBean bean, String postUrl, HqAuth auth) throws Exception {
        FormSession formSession = new FormSession(getFormXml(bean.getFormUrl(), auth),
                getRestoreXml(bean.getSessionData().getDomain(), auth),
                bean.getLang(), bean.getSessionData().getUsername(),
                bean.getSessionData().getDomain(), bean.getSessionData().getData(),
                postUrl, bean.getInstanceContent(), bean.getOneQuestionPerScreen());
        formSessionRepo.save(formSession.serialize());
        return new NewFormResponse(formSession);
    }

    public NewFormResponse getResponse(SerializableFormSession session)
            throws Exception {
        FormSession formSession = new FormSession(session);
        return new NewFormResponse(formSession);
    }

    private String getRestoreXml(String domain, HqAuth auth) {
        return restoreService.getRestoreXml(domain, auth);
    }

    private String getFormXml(String formUrl, HqAuth auth) {
        return xFormService.getFormXml(formUrl, auth);
    }
}
