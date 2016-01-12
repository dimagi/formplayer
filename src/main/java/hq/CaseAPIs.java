package hq;

import org.apache.tomcat.util.codec.binary.Base64;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.modern.parse.ParseUtilsHelper;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by willpride on 1/7/16.
 */
public class CaseAPIs {
    public static String getOTARestore(String username, String password){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange("https://www.commcarehq.org/a/ccqa/phone/restore/?version=2.0", HttpMethod.POST,
                        new HttpEntity<String>(createHeaders(username, password)), String.class);
        return response.getBody();
    }

    public static String getFullUsername(String username, String domain){
        // assumes commcarehq.org as server
        return username + "@" + domain + ".commcarehq.org";
    }

    public static HttpHeaders createHeaders(String username, String password ){
        return new HttpHeaders(){
            {
                String auth = username + ":" + password;
                byte[] encodedAuth = Base64.encodeBase64(
                        auth.getBytes(Charset.forName("US-ASCII")) );
                String authHeader = "Basic " + new String( encodedAuth );
                set( "Authorization", authHeader );
            }
        };
    }

    public static int restoreUser(String username, String password){
        String restorePayload = getOTARestore(username, password);
        UserSqlSandbox mSandbox = SqlSandboxUtils.getStaticStorage(username);
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        try {
            ParseUtilsHelper.parseXMLIntoSandbox(restorePayload, mSandbox);
            return mSandbox.getCaseStorage().getNumRecords();
        } catch (InvalidStructureException e) {
            e.printStackTrace();
        } catch (UnfullfilledRequirementsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch(XmlPullParserException e){
            e.printStackTrace();
        }
        return -1;
    }
}
