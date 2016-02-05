package requests;

import auth.BasicAuth;
import auth.HqAuth;
import beans.InstallRequestBean;
import beans.InstallResponseBean;
import beans.MenuResponseBean;
import com.sun.glass.ui.Menu;
import hq.CaseAPIs;
import install.FormplayerConfigEngine;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.tomcat.util.bcel.classfile.Constant;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.api.session.SessionWrapper;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.util.CommCareConfigEngine;
import org.commcare.util.cli.MenuScreen;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.util.externalizable.LivePrototypeFactory;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import services.RestoreService;
import services.XFormService;
import util.Constants;
import util.StringUtils;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willpride on 2/4/16.
 */
@Component
public class InstallRequest {
    XFormService xFormService;
    RestoreService restoreService;
    FormplayerConfigEngine engine;
    UserSqlSandbox sandbox;
    SessionWrapper sessionWrapper;
    HqAuth auth;
    String installReference;
    String username;
    String password;
    String domain;
    MenuDisplayable[] choices;

    @Value("${commcarehq.host}")
    String host;

    public InstallRequest(InstallRequestBean bean, XFormService xFormService, RestoreService restoreService) throws Exception {
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.installReference = bean.getInstallReference();
        this.username = bean.getUsername();
        this.password = bean.getPassword();
        this.domain = bean.getDomain();
        String domainedUsername = StringUtils.getFullUsername(username, domain, host);
        this.auth = new BasicAuth(domainedUsername, password);
        engine = configureApplication(installReference);
        sandbox = CaseAPIs.restoreIfNotExists(domainedUsername, restoreService, domain, auth);
        sessionWrapper = new SessionWrapper(engine.getPlatform(), sandbox);
        MenuScreen menuScreen = new MenuScreen();
        menuScreen.init(sessionWrapper);
        choices = menuScreen.getChoices();
    }

    public FormplayerConfigEngine configureApplication(String installReference){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FormplayerConfigEngine engine = new FormplayerConfigEngine(baos, username);
        if(installReference.endsWith(".ccz")){
            engine.initFromArchive(installReference);
        } else{
            throw new RuntimeException("Can't instantiate with reference: " + installReference);
        }
        engine.initEnvironment();
        return engine;
    }

    public String getInstallSource(){
        return xFormService.getFormXml(installReference, auth);
    }

    public String getRestoreXml(){
        return restoreService.getRestoreXml(domain, auth);
    }

    public MenuResponseBean getResponse(){
        MenuResponseBean menuResponseBean = new MenuResponseBean();
        menuResponseBean.setMenuType(Constants.MENU_MODULE);
        menuResponseBean.setOptions(parseMenuChoices(choices));
        return menuResponseBean;
    }

    public Map<Integer, String> parseMenuChoices(MenuDisplayable[] menuDisplayables){
        Map<Integer, String> ret = new HashMap<Integer, String>();
        for(int i = 0; i < menuDisplayables.length; i++){
            ret.put(new Integer(i), menuDisplayables[i].getDisplayText());
        }
        return ret;
    }
}
