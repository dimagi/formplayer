package application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import services.AuthService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * In order for the Formplayer frontend in HQ to query this API directly we need to enable CORS
 *
 * See http://stackoverflow.com/questions/13457772/cors-ajax-session-cookies-access-control-allow-credentials-withcredentials
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Autowired
    AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("sessionid")) {
                    authService.configureAuth(cookie.getValue());
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}