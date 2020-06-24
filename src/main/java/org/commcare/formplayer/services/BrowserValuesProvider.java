package org.commcare.formplayer.services;

import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.javarosa.core.model.utils.TimezoneProvider;

/**
 * Created by amstone326 on 1/8/18.
 */
public class BrowserValuesProvider extends TimezoneProvider {

    private int timezoneOffsetMillis = -1;

    public void setTimezoneOffset(AuthenticatedRequestBean bean) {
        timezoneOffsetMillis = bean.getTzOffset();
    }

    @Override
    public int getTimezoneOffsetMillis() {
        return timezoneOffsetMillis;
    }

}
