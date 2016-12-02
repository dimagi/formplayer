package application;

import auth.DjangoAuth;
import beans.CaseBean;
import beans.SessionRequestBean;
import beans.debugger.DebuggerFormattedQuestionsResponseBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import objects.SerializableFormSession;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import repo.SerializableMenuSession;
import services.FormattedQuestionsService;
import session.FormSession;
import session.MenuSession;
import util.Constants;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

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

        Hashtable<String, String> externalDataInstances = formSession.getPrettyExternalInstances();
        CaseBean[] cases = formSession.getCases();

        return new DebuggerFormattedQuestionsResponseBean(
                serializableMenuSession.getAppId(),
                formSession.getXmlns(),
                formSession.getInstanceXml(),
                response.getFormattedQuestions(),
                response.getQuestionList(),
                FunctionUtils.xPathFuncList(),
                externalDataInstances,
                cases
        );
    }

    private List<String> generateFunctionList() {
        List<Class> functionList = FunctionUtils.xPathFuncList();
        List<String> functionStrings = new ArrayList<>();
        for(Class clazz: functionList) {
            try {
                XPathFuncExpr function = (XPathFuncExpr) clazz.newInstance();
                int argCount = function.getExpectedArgCount();
                String name = function.getName();
                StringBuilder sb = new StringBuilder(name);
                sb.append("(");
                if(argCount > 2) {
                    for(int i = 0; i < argCount - 1; i ++) {
                        sb.append(" ,");
                    }
                    sb.append(" ");
                }
                sb.append(")");
                functionStrings.add(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return functionStrings;
    }
}
