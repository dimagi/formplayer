package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Created by willpride on 2/4/16.
 */
public class InstallRequestBean {
    private String installReference;
    private String username;
    private String password;
    private String domain;

    @JsonGetter(value = "install_reference")
    public String getInstallReference() {
        return installReference;
    }
    @JsonSetter(value = "install_reference")
    public void setInstallReference(String installReference) {
        this.installReference = installReference;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
