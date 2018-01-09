package services;

import org.javarosa.core.model.utils.TimezoneProvider;
import org.javarosa.core.model.utils.TimezoneProviderSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by amstone326 on 1/8/18.
 */
public class FormplayerTimezoneSource extends TimezoneProviderSource {

    @Autowired
    private BrowserValuesProvider browserValuesProvider;

    @Override
    protected TimezoneProvider getProvider() {
        System.out.println("returning provider from FormplayerTimezoneSource: " + browserValuesProvider);
        return browserValuesProvider;
    }
}
