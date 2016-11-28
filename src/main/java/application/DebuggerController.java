package application;

import auth.DjangoAuth;
import beans.SessionRequestBean;
import beans.debugger.DebuggerFormattedQuestionsResponseBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import objects.SerializableFormSession;
import org.javarosa.xpath.expr.FunctionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.SerializableMenuSession;
import services.FormattedQuestionsService;
import session.FormSession;
import session.MenuSession;
import util.Constants;

/**
 * Controller class for all routes pertaining to the CloudCare Debugger
 */
@Api(value = "Debugger Controller", description = "Operations involving the CloudCare Debugger")
@RestController
@EnableAutoConfiguration
public class DebuggerController extends AbstractBaseController {

    @Autowired
    private FormattedQuestionsService formattedQuestionsService;

    @ApiOperation(value = "Get formatted questions and instance xml")
    @RequestMapping(value = Constants.URL_DEBUGGER_FORMATTED_QUESTIONS, method = RequestMethod.POST)
    public DebuggerFormattedQuestionsResponseBean getFormattedQuesitons(
            @RequestBody SessionRequestBean debuggerRequest,
            @CookieValue(Constants.POSTGRES_DJANGO_SESSION_ID) String authToken) throws Exception {

        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(debuggerRequest.getSessionId());
        FormSession formSession = new FormSession(serializableFormSession);
        SerializableMenuSession serializableMenuSession = menuSessionRepo.findOne(serializableFormSession.getMenuSessionId());
        FormattedQuestionsService.QuestionResponse response = formattedQuestionsService.getFormattedQuestions(
                debuggerRequest.getDomain(),
                serializableMenuSession.getAppId(),
                formSession.getXmlns(),
                formSession.getInstanceXml(),
                new DjangoAuth(authToken)
        );
        return new DebuggerFormattedQuestionsResponseBean(
                serializableMenuSession.getAppId(),
                formSession.getXmlns(),
                formSession.getInstanceXml(),
                response.getFormattedQuestions(),
                response.getQuestionList(),
                FunctionUtils.xPathFuncList()
        );
    }

    @ApiOperation(value = "Evaluate the given XPath under the current context")
    @RequestMapping(value = Constants.URL_EVALUATE_XPATH, method = RequestMethod.POST)
    @ResponseBody
    @UserLock
    public EvaluateXPathResponseBean evaluateXpath(@RequestBody EvaluateXPathRequestBean evaluateXPathRequestBean) throws Exception {
        SerializableFormSession serializableFormSession = formSessionRepo.findOneWrapped(evaluateXPathRequestBean.getSessionId());
        FormSession formEntrySession = new FormSession(serializableFormSession);
        EvaluateXPathResponseBean evaluateXPathResponseBean =
                new EvaluateXPathResponseBean(formEntrySession, evaluateXPathRequestBean.getXpath());
        return evaluateXPathResponseBean;
    }

}
