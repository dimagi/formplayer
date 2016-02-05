package application;

import beans.InstallRequestBean;
import beans.InstallResponseBean;
import beans.MenuResponseBean;
import beans.MenuSelectBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RestController;
import requests.InstallRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import services.RestoreService;
import services.XFormService;
import util.Constants;

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

    @RequestMapping(Constants.URL_INSTALL)
    public MenuResponseBean performInstall(@RequestBody InstallRequestBean installRequestBean) throws Exception {
        InstallRequest installRequest = new InstallRequest(installRequestBean, xFormService, restoreService);
        return installRequest.getResponse();
    }
/*
    @RequestMapping(Constants.URL_MENU_SELECT)
    public MenuResponseBean selectMenu(@RequestBody MenuSelectBean menuSelectBean) throws Exception {
        //InstallRequest installRequest = new InstallRequest(menuSelectBean, xFormService, restoreService);
        //return installRequest.getResponse();
    }
    */
}
