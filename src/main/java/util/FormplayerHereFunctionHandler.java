package util;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.HereFunctionHandler;
import org.javarosa.core.model.data.GeoPointData;
import org.javarosa.xpath.XPathArityException;

/**
 * Created by amstone326 on 12/6/17.
 */
public class FormplayerHereFunctionHandler extends HereFunctionHandler {

    @Override
    public Object eval(Object[] args, EvaluationContext ec) throws XPathArityException {
        alertOnEval();
        if (listener.getLocation() == null) {
            return "";
        }
        String[] locationData = listener.getLocation().split(",");
        double lat = Double.parseDouble(locationData[0]);
        double lon = Double.parseDouble(locationData[1]);
        return (new GeoPointData(new double[]{lat, lon})).getDisplayText();
    }

}
