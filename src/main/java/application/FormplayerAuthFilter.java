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

    private boolean authorizeRequest(HttpServletRequest request){
        if(request.getCookies() !=  null) {
            for (Cookie cookie : request.getCookies()) {
                if(Constants.POSTGRES_DJANGO_SESSION_ID.equals(cookie.getName())){
                    return authorizeToken(cookie.getValue());
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

    private boolean authorizeToken(String value) {
        CouchUser user = new CouchUser();
        return user.isAuthorized(tokenRepo.getSessionToken(value));
    }

    @Override
    public void destroy() {

    }

    public void setResponseUnauthorized(HttpServletResponse response) {
        response.reset();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
