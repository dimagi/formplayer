package application;

import hq.interfaces.CouchUser;
import hq.models.PostgresUser;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import repo.TokenRepo;
import repo.impl.CouchUserRepo;
import repo.impl.PostgresUserRepo;
import util.Constants;
import util.FormplayerHttpRequest;
import util.RequestUtils;
import util.UserUtils;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter that determines whether a request needs to be authorized,
 * then attempts authorization by checking the auth token against
 * Django's sessionid table, returning an Unauthorized response if
 * appropriate
 *
 * @author wspride
 */
@Component
public class FormplayerAuthFilter extends OncePerRequestFilter {

    @Autowired
    TokenRepo tokenRepo;

    @Autowired
    PostgresUserRepo postgresUserRepo;

    @Autowired
    CouchUserRepo couchUserRepo;

    @Autowired
    RedisLockRegistry userLockRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        FormplayerHttpRequest request = new FormplayerHttpRequest((HttpServletRequest) req);
        if (isAuthorizationRequired(request)) {
            // These are order dependent
            if (getSessionId(request) == null) {
                setResponseUnauthorized(response, "Invalid session id");
                return;
            }
            setToken(request);
            setDomain(request);
            setUser(request);
            JSONObject data = RequestUtils.getPostData(request);
            if (!authorizeRequest(request, data.getString("domain"), getUsername(data))) {
                setResponseUnauthorized(response, "Invalid user");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String getUsername(JSONObject data) {
        try {
            return data.getString("username");
        } catch (JSONException e) {
            // TODO: Delete when no longer using HQ to proxy requests for Edit Forms
            return data.getJSONObject("session-data").getString("username");
        }
    }

    private void setToken(FormplayerHttpRequest request) {
        request.setToken(tokenRepo.getSessionToken(getSessionId(request)));
    }

    /**
     * Takes a request object and queries postgres and couch to get the corresponding
     * session user's.
     *
     * @param request
     */
    private void setUser(FormplayerHttpRequest request) {
        PostgresUser postgresUser;
        // Need to handle anonymous user workflow
        if (request.getToken().getUserId() == Constants.ANONYMOUS_DJANGO_USERID) {
            postgresUser = postgresUserRepo.getUserByUsername(
                    UserUtils.anonymousUsername(request.getDomain())
            );
        } else {
            postgresUser = postgresUserRepo.getUserByDjangoId(request.getToken().getUserId());
        }
        CouchUser couchUser = couchUserRepo.getUserByUsername(postgresUser.getUsername());

        request.setCouchUser(couchUser);
        request.setPostgresUser(postgresUser);
    }

    /**
     * Searches through the request cookie's to get the session id of the request.
     * @param request
     * @return The sessionid or null if not found
     */
    private String getSessionId(FormplayerHttpRequest request) {
        if(request.getCookies() !=  null) {
            for (Cookie cookie : request.getCookies()) {
                if(Constants.POSTGRES_DJANGO_SESSION_ID.equals(cookie.getName())){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setDomain(FormplayerHttpRequest request) {
        JSONObject data = RequestUtils.getPostData(request);
        if (data.getString("domain") == null) {
            throw new RuntimeException("No domain specified for the request: " + request.getRequestURI());
        }
        request.setDomain(data.getString("domain"));
    }

    /**
     * Currently, we want to auth every POST and GET request. In particular, we want to let OPTIONS
     * requests through since these don' have auth and we need them for CORS preflight
     * @param request the request to be authorized
     * @return request needs to be authorized
     */
    private boolean isAuthorizationRequired(HttpServletRequest request){
        String uri = StringUtils.strip(request.getRequestURI(), "/");
        for (Pattern pattern : Constants.AUTH_WHITELIST) {
            Matcher matcher = pattern.matcher(uri);
            if (matcher.matches()) {
                return false;
            }
        }

        return (request.getMethod().equals("POST") || request.getMethod().equals("GET"));
    }

    /**
     * This function ensures that the request session's user and domain matches the user and domain
     * sent in the body of the POST request. Note, superusers are able to authenticate
     * as other users.
     * @param request
     * @param domain
     * @param username
     * @return true if authorized, false otherwise
     */
    private boolean authorizeRequest(FormplayerHttpRequest request, String domain, String username) {
        if (request.getToken() == null) {
            return false;
        }
        // Check session token is expired
        if (request.getToken().getExpireDate().before(new java.util.Date())){
            return false;
        }

        // Ensure that we actually have a couch user
        if (request.getCouchUser() == null) {
            return false;
        }

        // Ensure domain and username match couch user
        return request.getCouchUser().isAuthorized(domain, username);
    }

    @Override
    public void destroy() {

    }

    public void setResponseUnauthorized(HttpServletResponse response, String message) {
        response.reset();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        PrintWriter writer = null;
        JSONObject responseJSON = new JSONObject();
        responseJSON.put("error", message);
        try {
            writer = response.getWriter();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write response", e);
        }
        writer.write(responseJSON.toString());
        writer.flush();
        writer.close();
    }

}
