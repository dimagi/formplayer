package org.commcare.formplayer.beans;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class OpenRosaResponse {

    OpenRosaResponse() {}

    @Element
    private String message;

    public String getMessage() {
        return message;
    }
}