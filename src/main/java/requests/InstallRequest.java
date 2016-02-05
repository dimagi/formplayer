package requests;

import auth.BasicAuth;
import auth.HqAuth;
import beans.InstallRequestBean;
import beans.InstallResponseBean;
import beans.MenuResponseBean;
import com.sun.glass.ui.Menu;
import hq.CaseAPIs;
import install.FormplayerConfigEngine;
import objects.SerializableMenuSession;
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
import repo.MenuSessionRepo;
import services.RestoreService;
import services.XFormService;
import session.FormEntrySession;
import session.MenuSession;
import util.Constants;
import util.StringUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by willpride on 2/4/16.
 */
@Component
public class InstallRequest {
    XFormService xFormService;
    RestoreService restoreService;
    HqAuth auth;
    String installReference;
    String username;
    String password;
    String domain;
    MenuSession menuSession;

    @Value("${commcarehq.host}")
    String host;

    public InstallRequest(InstallRequestBean bean, XFormService xFormService, RestoreService restoreService) throws Exception {
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.installReference = bean.getInstallReference();
        this.username = bean.getUsername();
        this.password = bean.getPassword();
        this.domain = bean.getDomain();
        this.menuSession = new MenuSession(this.username, this.password, this.domain, this.host,
                this.installReference, this.restoreService);
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
        menuResponseBean.setOptions(parseMenuChoices(this.menuSession.getChoices()));
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
