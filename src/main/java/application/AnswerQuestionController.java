package application;

import beans.AnswerQuestionBean;
import beans.AnswerQuestionResponseBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import objects.SerializableSession;
import org.apache.commons.io.IOUtils;
import org.commcare.api.json.AnswerQuestionJson;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryModel;
import org.javarosa.xform.parse.XFormParser;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import repo.SessionRepo;
import requests.NewFormRequest;
import services.XFormService;

/**
 * Created by willpride on 1/20/16.
 */
@RestController
@EnableAutoConfiguration
public class AnswerQuestionController {

    @Autowired
    private SessionRepo sessionRepo;

    @Autowired
    private XFormService xFormService;

}
