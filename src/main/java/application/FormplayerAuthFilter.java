package application;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;
import repo.TokenRepo;
import util.Constants;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter that determines whether a request needs to be authorized,
 * then attempts authorization by checking the auth token against
 * Django's sessionid table, returning an Unauthorized response if
 * appropriate
 *
 * @author wspride
 */
@Component
public class FormplayerAuthFilter implements Filter {

    @Autowired
    TokenRepo tokenRepo;

    @Autowired
    PostgresUserRepo postgresUserRepo;

    @Autowired
    CouchUserRepo couchUserRepo;

    @Autowired
    RedisLockRegistry userLockRegistry;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        if (isAuthorizationRequired(request)) {
            if (!authorizeRequest(request)) {
                setResponseUnauthorized((HttpServletResponse) res);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private boolean authorizeRequest(ContentCachingRequestWrapper request){
        JSONObject data = null;
        try {
            data = new JSONObject(RequestUtils.getBody(request));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(request.getCookies() !=  null) {
            for (Cookie cookie : request.getCookies()) {
                if(Constants.POSTGRES_DJANGO_SESSION_ID.equals(cookie.getName())){
                    return authorizeToken(data.getString("domain"), data.getString("username"), cookie.getValue());
                }
            }
        }
        return false;
    }

    /**
     * Currently, we want to auth every POST and GET request. In particular, we want to let OPTIONS
     * requests through since these don' have auth and we need them for CORS preflight
     * @param request the request to be authorized
     * @return request needs to be authorized
     */
    private boolean isAuthorizationRequired(HttpServletRequest request){
        String uri = StringUtils.strip(request.getRequestURI(), "/");
        if (uri.equals(Constants.URL_SERVER_UP)) {
            return false;
        }
        return (request.getMethod().equals("POST") || request.getMethod().equals("GET"));
    }

    private boolean authorizeToken(String domain, String username, String value) {
        SessionToken token = tokenRepo.getSessionToken(value);
        if (token == null) {
            return false;
        }
        // Check session token is expired
        if (token.getExpireDate().before(new java.util.Date())){
            return false;
        }

        // Ensure domain and username match couch user
        PostgresUser postgresUser = postgresUserRepo.getUserByDjangoId(token.getUserId());
        return couchUserRepo.getUserByUsername(postgresUser.getUsername()).isAuthorized(domain, username);
    }

    @Override
    public void destroy() {

    }

    public void setResponseUnauthorized(HttpServletResponse response) {
        response.reset();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

}
