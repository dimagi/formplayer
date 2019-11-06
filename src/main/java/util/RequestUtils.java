package util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility function to deal with request objects.
 */
public class RequestUtils {

    // Logic taken from here:
    // http://stackoverflow.com/a/14885950/835696
    public static String getBody(HttpServletRequest request) throws IOException {

        String body = null;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                char[] charBuffer = new char[128];
                int bytesRead = -1;
                while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            } else {
                stringBuilder.append("");
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
        }

        body = stringBuilder.toString();
        return body;
    }

    public static JSONObject getPostData(FormplayerHttpRequest request) {
        JSONObject data = null;
        try {
            data = new JSONObject(getBody(request));
        } catch (IOException | JSONException a) {
            throw new RuntimeException("Unreadable POST Body for the request: " + request.getRequestURI());
        }
        return data;
    }

    public static String getRequestEndpoint(HttpServletRequest request) {
        return StringUtils.strip(request.getRequestURI(), "/");
    }

    /**
     * Get the HMAC hash of a given request body with a given key
     * Used by Formplayer to validate requests from HQ using shared internal key `commcarehq.formplayerAuthKey`
     */
    public static String getHmac(String key, String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
    }
}
