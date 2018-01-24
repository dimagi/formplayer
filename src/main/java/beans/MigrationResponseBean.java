package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Returns true if migration succeeded
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MigrationResponseBean {

    // our JSON-Object mapping lib (Jackson) requires a default constructor
    public MigrationResponseBean(){}

    @Override
    public String toString(){
        return "ServerUpBean ✓";
    }

    public String getStatus() {
        return "ok";
    }
}
