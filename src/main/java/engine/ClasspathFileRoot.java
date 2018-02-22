package engine;

import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.reference.ReferenceFactory;
import org.javarosa.core.reference.ReferenceManager;

/**
 * Created by willpride on 1/4/17.
 */
public class ClasspathFileRoot implements ReferenceFactory {

    @Override
    public Reference derive(String URI) throws InvalidReferenceException {
        return new ClasspathFileReference(URI.substring("jr://springfile/".length()));
    }

    @Override
    public Reference derive(String URI, String context) throws InvalidReferenceException {
        if (context.lastIndexOf('/') != -1) {
            context = context.substring(0, context.lastIndexOf('/') + 1);
        }
        return ReferenceManager.instance().DeriveReference(context + URI);
    }

    @Override
    public boolean derives(String URI) {
        return URI.toLowerCase().startsWith("jr://springfile/");
    }
}