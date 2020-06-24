package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Returns true if the server is up and healthy
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerUpBean {

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public ServerUpBean(){}

    @Override
    public String toString(){
        return "ServerUpBean ✓";
    }

    public String getStatus() {
        return "ok";
    }
}
