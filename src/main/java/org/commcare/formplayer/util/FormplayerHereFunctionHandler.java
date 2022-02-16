package org.commcare.formplayer.util;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.HereFunctionHandler;
import org.javarosa.core.model.condition.HereFunctionHandlerListener;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathTypeMismatchException;

/**
 * Created by amstone326 on 12/6/17.
 */
public class FormplayerHereFunctionHandler extends HereFunctionHandler {

    private final String browserLocation;

    public FormplayerHereFunctionHandler(HereFunctionHandlerListener listener,
            String browserLocation) {
        super();
        registerListener(listener);
        this.browserLocation = browserLocation;
    }

    @Override
    public Object eval(Object[] args, EvaluationContext ec) throws XPathArityException {
        alertOnEval();
        if (browserLocation == null) {
            return "";
        }
        try {
            String[] locationData = browserLocation.split(",");
            double lat = Double.parseDouble(locationData[0]);
            double lon = Double.parseDouble(locationData[1]);
            return (new GeoPointData(new double[]{lat, lon})).getDisplayText();
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new XPathTypeMismatchException(
                    "The browser location provided for evaluation of here() was of an unexpected "
                            + "format: "
                            + browserLocation);
        }
    }

}
