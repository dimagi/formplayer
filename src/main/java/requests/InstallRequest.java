package requests;

import beans.InstallRequestBean;
import beans.MenuResponseBean;
import org.springframework.stereotype.Component;
import repo.MenuRepo;
import services.InstallService;
import services.RestoreService;
import session.MenuSession;
import util.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by willpride on 2/4/16.
 */
@Component
public class InstallRequest {
    MenuSession menuSession;

    public InstallRequest(InstallRequestBean bean, RestoreService restoreService,
                          MenuRepo menuSessionRepo, InstallService installService) throws Exception {
        this.menuSession = new MenuSession(bean.getUsername(), bean.getPassword(), bean.getDomain(),
                bean.getInstallReference(), null, restoreService, null, null, installService);
        menuSessionRepo.save(menuSession.serialize());
    }

    public MenuResponseBean getResponse(){
        MenuResponseBean menuResponseBean = new MenuResponseBean();
        menuResponseBean.setMenuType(Constants.MENU_MODULE);
        menuResponseBean.setOptions(parseMenuChoices(this.menuSession.getChoices()));
        menuResponseBean.setSessionId(this.menuSession.getSessionId());
        return menuResponseBean;
    }

    public Map<Integer, String> parseMenuChoices(String[] menuDisplayables){
        Map<Integer, String> ret = new HashMap<Integer, String>();
        for(int i = 0; i < menuDisplayables.length; i++){
            ret.put(new Integer(i), menuDisplayables[i]);
        }
        return ret;
    }
}
