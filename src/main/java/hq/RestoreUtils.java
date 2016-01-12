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
 * Created by willpride on 1/12/16.
 */
public class RestoreUtils {

    public static String getOTARestoreAuth(String username, String authKey){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange("http://localhost:8000/a/test/phone/restore/?version=2.0", HttpMethod.GET,
                        new HttpEntity<String>(HttpUtils.createAuthHeaders(username, authKey)), String.class);
        return response.getBody();
    }

    public static String getOTARestore(String username, String password){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response =
                restTemplate.exchange("https://www.commcarehq.org/a/ccqa/phone/restore/?version=2.0", HttpMethod.POST,
                        new HttpEntity<String>(HttpUtils.createHeaders(username, password)), String.class);
        return response.getBody();
    }

    public static UserSqlSandbox restoreUser(String username, String password) throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        String restorePayload = getOTARestore(username, password);
        UserSqlSandbox mSandbox = SqlSandboxUtils.getStaticStorage(username);
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        ParseUtilsHelper.parseXMLIntoSandbox(restorePayload, mSandbox);
        return mSandbox;
    }

    public static UserSqlSandbox restoreUserAuth(String username, String authKey) throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        String restorePayload = getOTARestoreAuth(username, authKey);
        UserSqlSandbox mSandbox = SqlSandboxUtils.getStaticStorage(username);
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        ParseUtilsHelper.parseXMLIntoSandbox(restorePayload, mSandbox);
        return mSandbox;
    }
}
