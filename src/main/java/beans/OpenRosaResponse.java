package beans;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class OpenRosaResponse {

    OpenRosaResponse() {}

    @JacksonXmlProperty(localName = "message")
    private String message;

    public String getMessage() {
        return message;
    }
}