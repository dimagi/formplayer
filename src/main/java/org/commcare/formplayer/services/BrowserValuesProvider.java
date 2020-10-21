package org.commcare.formplayer.services;

import io.sentry.event.Event;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
import org.commcare.formplayer.util.FormplayerSentry;
import org.javarosa.core.model.utils.TimezoneProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by amstone326 on 1/8/18.
 */
public class BrowserValuesProvider extends TimezoneProvider {

    @Autowired
    protected FormplayerSentry raven;

    private int timezoneOffsetMillis = -1;
    private TimeZone timezoneFromBrowser = null;

    public void setTimezoneOffset(AuthenticatedRequestBean bean) {
        timezoneOffsetMillis = bean.getTzOffset();
        String tzFromBrowserString = bean.getTzFromBrowser();

        if (tzFromBrowserString != null &&
                Arrays.asList(TimeZone.getAvailableIDs()).contains(tzFromBrowserString)) {
            timezoneFromBrowser = TimeZone.getTimeZone(tzFromBrowserString);
        }

        try {
            checkTzDiscrepancy(timezoneFromBrowser, timezoneOffsetMillis);
        } catch (TzDiscrepancyException e) {
            raven.sendRavenException(e, Event.Level.WARNING);
        }
    }

    public void checkTzDiscrepancy(TimeZone tz, int reportedTzOffsetMillis) throws TzDiscrepancyException {
        if (tz == null && reportedTzOffsetMillis == -1) {
            return;
        }
        int tzOffsetFromTz = 0;
        if (tz != null) {
            tzOffsetFromTz = tz.getOffset(new Date().getTime());
            if (tzOffsetFromTz == reportedTzOffsetMillis) {
                return;
            }
        }
        String tzName = (tz == null) ? null : tz.getDisplayName();
        String errorMsg = String.format("Reported timezone %s has offset %d which is different than reported" +
                "offset %d", tzName, tzOffsetFromTz, reportedTzOffsetMillis);
        throw new TzDiscrepancyException(errorMsg);
    }

    @Override
    public int getTimezoneOffsetMillis() {
        return timezoneOffsetMillis;
    }

    @Override
    public TimeZone getTimezone() {
        return timezoneFromBrowser;
    }

    public class TzDiscrepancyException extends Exception {
        public TzDiscrepancyException(String message) {
            super(message);
        }
    }

}
