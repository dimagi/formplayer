package org.commcare.formplayer.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date utility functions for dealing with dates in formplayer
 */
public class FormplayerDateUtils {
    public static String convertJavaDateStringToISO(String date) {
        DateFormat dfFrom = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy", Locale.ENGLISH);
        Date result;
        try {
            result =  dfFrom.parse(date);
        } catch (ParseException e) {
            // Could not parse date
            return null;
        }
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat dfTo = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dfTo.setTimeZone(tz);
        return dfTo.format(result);
    }

    // Remove once all Old CloudCare sessions are migrated
    public static String convertIsoToJavaDate(String date) {
        DateFormat dfFrom = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSSX", Locale.ENGLISH);
        Date result;
        try {
            result =  dfFrom.parse(date);
        } catch (ParseException e) {
            // Could not parse date
            return null;
        }
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat dfTo = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy");
        dfTo.setTimeZone(tz);
        return dfTo.format(result);
    }
}
