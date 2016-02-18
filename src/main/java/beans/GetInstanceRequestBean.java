package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by willpride on 1/20/16.
 * Get the current instance of the specified session
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetInstanceRequestBean extends SessionBean{

    public GetInstanceRequestBean(){}

    @Override
    public String toString(){
        return "GetInstanceRequestBean [sessionId=" + sessionId +"]";
    }
}
