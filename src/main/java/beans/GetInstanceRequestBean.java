package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Map;

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
