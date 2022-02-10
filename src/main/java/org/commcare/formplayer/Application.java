package org.commcare.formplayer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.application.RequestResponseLoggingFilter;
import org.commcare.formplayer.repo.FormplayerBaseJpaRepoImpl;
import org.commcare.formplayer.util.PrototypeUtils;
import org.javarosa.core.reference.ReferenceHandler;
import org.javarosa.core.services.locale.LocalizerManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SpringBootApplication
@EnableJpaRepositories(
        basePackages = {"org.commcare.formplayer.repo"},
        repositoryBaseClass = FormplayerBaseJpaRepoImpl.class
)
@EntityScan("org.commcare.formplayer.objects")
@EnableCaching
public class Application {

    // Allows logging of sensitive data.
    @Value("${sensitiveData.enableLogging:false}")
    private boolean enableSensitiveLogging;

    private final Log log = LogFactory.getLog(Application.class);

    public static void main(String[] args) {
        PrototypeUtils.setupThreadLocalPrototypes();
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        LocalizerManager.setUseThreadLocalStrategy(true);
        ReferenceHandler.setUseThreadLocalStrategy(true);
    }

    @Bean
    public Filter reqRespLoggingFilter() {
        return new RequestResponseLoggingFilter(null, enableSensitiveLogging);
    }
}
