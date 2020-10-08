package org.commcare.formplayer.services;

import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.javarosa.core.model.utils.TimezoneProvider;
import java.util.TimeZone;
import java.util.Calendar;

/**
 * Created by amstone326 on 1/8/18.
 */
public class BrowserValuesProvider extends TimezoneProvider {

    private int timezoneOffsetMillis = -1;
    private TimeZone timezoneFromBrowser = null;

    public void setTimezoneOffset(AuthenticatedRequestBean bean) {
        try {
            timezoneFromBrowser = TimeZone.getTimeZone(bean.getTzFromBrowser());
        } catch (Exception e) {
            // TODO: handle and make more specific.
        }

        timezoneOffsetMillis = bean.getTzOffset();
    }

    @Override
    public int getTimezoneOffsetMillis() {
        return timezoneOffsetMillis;
    }

    @Override
    public TimeZone getTimezone() {
        return timezoneFromBrowser;
    }

}
