package engine;

import org.javarosa.core.reference.Reference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by willpride on 1/4/17.
 */
public class ClasspathFileReference implements Reference {

    private final String assetURI;

    public ClasspathFileReference(String assetURI) {
        this.assetURI = assetURI;
    }

    @Override
    public boolean doesBinaryExist() throws IOException {
        Resource resource = new ClassPathResource(assetURI);
        return resource.exists();
    }

    @Override
    public InputStream getStream() throws IOException {
        Resource resource = new ClassPathResource(assetURI);
        return resource.getInputStream();
    }

    @Override
    public String getURI() {
        return "jr://springfile/" + assetURI;
    }

    @Override
    public String getLocalURI() {
        return null;
    }

    @Override
    public boolean isReadOnly() {
        //I think this is always true, not 100% sure.
        return true;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Asset references are read only!");
    }

    @Override
    public void remove() throws IOException {
        //IOException? Do we use this for certain forms of installers? Probably not.
        throw new IOException("Cannot remove Asset files from the Package");
    }
}

