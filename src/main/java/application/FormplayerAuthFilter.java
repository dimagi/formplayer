package application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import repo.TokenRepo;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by willpride on 6/27/16.
 */
@Component
public class FormplayerAuthFilter implements Filter {

    @Autowired
    TokenRepo tokenRepo;

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
                if("sessionid".equals(cookie.getName())){
                    return authorizeToken(cookie.getValue());
                }
            }
        }
        return false;
    }

    private boolean isAuthorizationRequired(HttpServletRequest request){
        if(request.getMethod().equals("POST")){
            return true;
        }
        return false;
    }

    private boolean authorizeToken(String value) {
        return tokenRepo.isAuthorized(value);
    }

    @Override
    public void destroy() {

    }

    public void setResponseUnauthorized(HttpServletResponse response) {
        response.reset();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
