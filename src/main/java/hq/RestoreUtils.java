package hq;

import org.apache.tomcat.util.codec.binary.Base64;
import org.commcare.api.persistence.SqlSandboxUtils;
import org.commcare.api.persistence.UserSqlSandbox;
import org.commcare.modern.parse.ParseUtilsHelper;
import org.javarosa.core.api.ClassNameHasher;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.xmlpull.v1.XmlPullParserException;
import requests.FilterRequest;
import requests.RestoreRequest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.logging.Filter;

/**
 * Created by willpride on 1/12/16.
 */
public class RestoreUtils {

    public static UserSqlSandbox restoreUser(String username, String restorePayload) throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        UserSqlSandbox mSandbox = SqlSandboxUtils.getStaticStorage(username);
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        ParseUtilsHelper.parseXMLIntoSandbox(restorePayload, mSandbox);
        return mSandbox;
    }

    public static UserSqlSandbox restoreUser(RestoreRequest restoreRequest, String restorePayload) throws
            UnfullfilledRequirementsException, InvalidStructureException, IOException, XmlPullParserException {
        UserSqlSandbox mSandbox = SqlSandboxUtils.getStaticStorage(restoreRequest.getUsername());
        PrototypeFactory.setStaticHasher(new ClassNameHasher());
        ParseUtilsHelper.parseXMLIntoSandbox(restorePayload, mSandbox);
        return mSandbox;
    }
}
