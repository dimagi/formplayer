package requests;

import beans.InstallRequestBean;
import org.springframework.stereotype.Component;
import services.InstallService;
import services.RestoreService;
import session.MenuSession;

/**
 * Created by willpride on 2/4/16.
 */
@Component
public class InstallRequest {
    MenuSession menuSession;

    public InstallRequest(InstallRequestBean bean,
                          RestoreService restoreService,
                          InstallService installService) throws Exception {
        this.menuSession = new MenuSession(bean.getUsername(), bean.getPassword(), bean.getDomain(), bean.getAppId(),
                bean.getInstallReference(), installService, restoreService);
    }

    public MenuSession getMenuSession(){
        return menuSession;
    }
}
