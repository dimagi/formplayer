package utils;

import objects.SerializableFormSession;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import services.*;
import services.impl.InstallServiceImpl;
import services.impl.SubmitServiceImpl;

@Configuration
public class TestContext {
 
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
 
        messageSource.setBasename("i18n/messages");
        messageSource.setUseCodeAsDefaultMessage(true);
 
        return messageSource;
    }

    @Bean
    public InternalResourceViewResolver viewResolver(){
        InternalResourceViewResolver internalResourceViewResolver = new InternalResourceViewResolver();
        internalResourceViewResolver.setPrefix("/WEB-INF/jsp/view/");
        internalResourceViewResolver.setSuffix(".jsp");
        return internalResourceViewResolver;
    }

    @Bean
    public FormSessionRepo formSessionRepo() {
        return Mockito.mock(FormSessionRepo.class);
    }


    @Bean
    public MenuSessionRepo menuSessionRepo() {
        return Mockito.mock(MenuSessionRepo.class);
    }

    @Bean
    public XFormService newFormRequest() {
        return Mockito.mock(XFormService.class);
    }

    @Bean
    public RestoreFactory restoreFactory() {
        return Mockito.spy(RestoreFactory.class);
    }

    @Bean
    public InstallService installService(){
        return Mockito.mock(InstallServiceImpl.class);
    }

    @Bean
    public SubmitService submitService() {
        return Mockito.mock(SubmitServiceImpl.class);
    }

    @Bean
    public LockRegistry userLockRegistry() {
        return Mockito.mock(LockRegistry.class);
    }

    @Bean
    public NewFormResponseFactory newFormResponseFactory(){
        return Mockito.mock(NewFormResponseFactory.class);
    }
}