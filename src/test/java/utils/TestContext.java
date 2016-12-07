package utils;

import installers.FormplayerInstallerFactory;
import mocks.MockFormSessionRepo;
import mocks.MockLockRegistry;
import mocks.MockMenuSessionRepo;
import mocks.TestInstallService;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import services.*;
import services.impl.SubmitServiceImpl;

@Configuration
public class TestContext {

    @Value("${redis.hostname}")
    private String redisHostName;

    public TestContext() {
        MockitoAnnotations.initMocks(this);
    }

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
        return Mockito.spy(MockFormSessionRepo.class);
    }

    @Bean
    public MenuSessionRepo menuSessionRepo() {
        return Mockito.spy(MockMenuSessionRepo.class);
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
    public FormplayerStorageFactory storageFactory() {
        return Mockito.spy(FormplayerStorageFactory.class);
    }

    @Bean
    public InstallService installService(){
        return Mockito.spy(TestInstallService.class);
    }

    @Bean
    public SubmitService submitService() {
        return Mockito.mock(SubmitServiceImpl.class);
    }

    @Bean
    public LockRegistry userLockRegistry() {
        return Mockito.spy(MockLockRegistry.class);
    }

    @Bean
    public NewFormResponseFactory newFormResponseFactory(){
        return Mockito.spy(NewFormResponseFactory.class);
    }

    @Bean
    public FormplayerInstallerFactory installerFactory() {
        return Mockito.spy(FormplayerInstallerFactory.class);
    }
}