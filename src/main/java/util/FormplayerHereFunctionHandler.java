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
        return (new GeoPointData(new double[]{0, 0, 0 ,0})).getDisplayText();
    }

}
