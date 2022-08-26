package org.commcare.formplayer.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

/**
 * Utility function to deal with request objects.
 */
public class RequestUtils {

    // Logic taken from here:
    // http://stackoverflow.com/a/14885950/835696
    public static String getBody(InputStream inputStream) throws IOException {
        String body;
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
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

    public static JSONObject getPostData(HttpServletRequest request) {
        JSONObject data = null;
        try {
            data = new JSONObject(getJsonBody(request));
        } catch (IOException | JSONException | ServletException a) {
            throw new RuntimeException(
                    "Unreadable POST Body for the request: " + request.getRequestURI(), a);
        }
        return data;
    }

    public static String getRequestEndpoint() {
        HttpServletRequest request = RequestUtils.getCurrentRequest();
        return request == null ? "unknown" : StringUtils.strip(request.getRequestURI(), "/");
    }

    /**
     * Get the HMAC hash of a given request body with a given key Used by Formplayer to validate
     * requests from HQ using shared internal key `commcarehq.formplayerAuthKey`
     */
    public static String getHmac(String key, String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
    }

    public static HttpServletRequest getCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return ((ServletRequestAttributes)attributes).getRequest();
        }
        return null;
    }

    public static Optional<HqUserDetailsBean> getUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            HqUserDetailsBean userDetails = (HqUserDetailsBean)authentication.getPrincipal();
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    /**
     * @return True if there is request in the context AND the request was authenticated with HMAC
     * auth
     */
    public static boolean requestAuthedWithHmac() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return false;
        }
        Object attribute = request.getAttribute(Constants.HMAC_REQUEST_ATTRIBUTE);
        return attribute != null && (Boolean)attribute;
    }

    // If a multipart request returns the part having content type as 'application/json',
    // otherwise the whole request content
    private static String getJsonBody(HttpServletRequest request) throws IOException, ServletException {
        String primaryContentType = request.getContentType().split(";")[0];
        if (primaryContentType.contentEquals(MediaType.MULTIPART_FORM_DATA_VALUE)) {
            for (Part part : request.getParts()) {
                if (part.getContentType().contentEquals(MediaType.APPLICATION_JSON_VALUE)) {
                    return getBody(part.getInputStream());
                }
            }
            throw new RuntimeException("No part found with content type " + MediaType.APPLICATION_JSON_VALUE
                    + " for multipart request " + request.getRequestURI());
        } else {
            return getBody(request.getInputStream());
        }
    }
}
