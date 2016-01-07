package hello;

import org.javarosa.core.model.instance.FormInstance;

public class Greeting {

    private FormInstance derp;

    private final long id;
    private final String content;

    public Greeting(long id, String content) {
        this.id = id;
        this.content = content;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }
}
