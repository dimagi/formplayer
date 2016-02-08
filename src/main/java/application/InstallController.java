package application;

import beans.InstallRequestBean;
import beans.MenuResponseBean;
import beans.MenuSelectBean;
import org.commcare.suite.model.MenuDisplayable;
import org.commcare.util.cli.MenuScreen;
import org.commcare.util.cli.Screen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RestController;
import repo.MenuRepo;
import requests.InstallRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import services.RestoreService;
import services.XFormService;
import session.MenuSession;
import util.Constants;

import java.util.HashMap;

/**
 * Created by willpride on 2/4/16.
 */
@RestController
@EnableAutoConfiguration
public class InstallController {

    @Autowired
    private XFormService xFormService;

    @Autowired
    private RestoreService restoreService;

    @Autowired
    private MenuRepo menuRepo;

    @RequestMapping(Constants.URL_INSTALL)
    public MenuResponseBean performInstall(@RequestBody InstallRequestBean installRequestBean) throws Exception {
        InstallRequest installRequest = new InstallRequest(installRequestBean, xFormService, restoreService, menuRepo);
        return installRequest.getResponse();
    }

    @RequestMapping(Constants.URL_MENU_SELECT)
    public MenuResponseBean selectMenu(@RequestBody MenuSelectBean menuSelectBean) throws Exception {
        MenuSession menuSession = new MenuSession(menuRepo.find(menuSelectBean.getSessionId()), restoreService);
        Screen nextScreen = menuSession.handleInput(menuSelectBean.getSelection());
        menuRepo.save(menuSession.serialize());
        if(nextScreen instanceof MenuScreen){
            MenuScreen menuScreen = (MenuScreen) nextScreen;
            MenuDisplayable[] options = menuScreen.getChoices();
            HashMap<Integer, String> optionsStrings = new HashMap<Integer, String>();
            for(int i=0; i <options.length; i++){
                optionsStrings.put(i, options[i].getDisplayText());
            }
            MenuResponseBean menuResponseBean = new MenuResponseBean();
            menuResponseBean.setMenuType(Constants.MENU_MODULE);
            menuResponseBean.setOptions(optionsStrings);
            return menuResponseBean;
        } else if (nextScreen == null){

        }
        return null;
    }

}
