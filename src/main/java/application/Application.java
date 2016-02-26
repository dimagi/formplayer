package application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@ComponentScan
@EnableAutoConfiguration
@EnableWebMvc
@Component
public class Application {

    Log log = LogFactory.getLog(Application.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter crlf = new CommonsRequestLoggingFilter();
        crlf.setIncludeClientInfo(true);
        crlf.setIncludeQueryString(true);
        crlf.setIncludePayload(true);
        return crlf;
    }

    public class ResponseLoggingFilter extends GenericFilterBean {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            final HttpServletResponse httpResponse = (HttpServletResponse) response;
            log.info("Got request URL: " + httpRequest.getRequestURL() + " , response code: " + httpResponse.getStatus());
            filterChain.doFilter(request, response);
        }
    }

    @Bean
    public Filter loggingFilter() {
        ResponseLoggingFilter loggingFilter = new ResponseLoggingFilter();
        return loggingFilter;
    }
}