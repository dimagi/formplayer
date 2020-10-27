package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Request to encrypt a string with a specified algorithm and key.
 *
 */
public class EncryptStringRequestBean extends AuthenticatedRequestBean {
    private String message;
    private String key;
    private String algorithm;

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public EncryptStringRequestBean(){}

    @JsonGetter(value = "message")
    public String getMessage() {
        return message;
    }
    @JsonSetter(value = "message")
    public void setMessage(String message) {
        if (message != null) {
            this.message = message;
        }
    }

    @JsonGetter(value = "key")
    public String getKey() {
        return key;
    }
    @JsonSetter(value = "key")
    public void setKey(String key) {
        if (key != null) {
            this.key = key;
        }
    }

    @JsonGetter(value = "algorithm")
    public String getAlgorithm() {
        return algorithm;
    }
    @JsonSetter(value = "algorithm")
    public void setAlgorithm(String algorithm) {
        if (algorithm != null) {
            this.algorithm = algorithm;
        }
    }

    @Override
    public String toString(){
        return "EncryptStringRequestBean [message=" + message + ", key=" + key + ", algorithm=" + algorithm + "]";
    }
}
