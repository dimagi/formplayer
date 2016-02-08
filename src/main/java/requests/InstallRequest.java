package requests;

import auth.HqAuth;
import beans.InstallRequestBean;
import beans.MenuResponseBean;
import beans.MenuSelectBean;
import org.commcare.suite.model.MenuDisplayable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import repo.MenuRepo;
import services.RestoreService;
import services.XFormService;
import session.MenuSession;
import util.Constants;

import java.util.HashMap;
import java.util.Map;

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

    public InstallRequest(InstallRequestBean bean, XFormService xFormService,
                          RestoreService restoreService, MenuRepo menuSessionRepo) throws Exception {
        this.xFormService = xFormService;
        this.restoreService = restoreService;
        this.installReference = bean.getInstallReference();
        this.username = bean.getUsername();
        this.password = bean.getPassword();
        this.domain = bean.getDomain();
        this.menuSession = new MenuSession(this.username, this.password, this.domain,
                this.installReference, null, this.restoreService, null);
        System.out.println("Savving session: " + menuSession);
        menuSessionRepo.save(menuSession.serialize());
        System.out.println("Save Session: " + menuSession);
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
        menuResponseBean.setSessionId(this.menuSession.getSessionId());
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
