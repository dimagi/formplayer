package services;

import beans.AuthenticatedRequestBean;
import beans.SessionNavigationBean;
import org.javarosa.core.model.utils.TimezoneProvider;

/**
 * Created by amstone326 on 1/8/18.
 */
public class BrowserValuesProvider extends TimezoneProvider {

    private String browserLocation;
    private int timezoneOffsetMillis = -1;

    public void setLocation(SessionNavigationBean bean) {
        browserLocation = bean.getGeoLocation();
    }

    public void setTimezoneOffset(AuthenticatedRequestBean bean) {
        timezoneOffsetMillis = bean.getTzOffset();
    }

    @Override
    public int getTimezoneOffsetMillis() {
        return timezoneOffsetMillis;
    }

}
