package utils;

import auth.HqAuth;
import objects.SerializableMenuSession;
import objects.SerializableSession;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import repo.MenuRepo;
import repo.SessionRepo;
import requests.NewFormRequest;
import services.RestoreService;
import services.XFormService;
import services.impl.RestoreServiceImpl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@Configuration
public class TestContext {

    public static SerializableSession serializableSession;
    public static SerializableMenuSession serializableMenuSession;
 
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
    public SessionRepo sessionRepo() {
        SessionRepo sessionRepo = Mockito.mock(SessionRepo.class);
        when(sessionRepo.find(anyString())).thenReturn(serializableSession);
        ArgumentCaptor<SerializableSession> argumentCaptor = ArgumentCaptor.forClass(SerializableSession.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableSession toBeSaved = (SerializableSession) args[0];
                serializableSession.setInstanceXml(toBeSaved.getInstanceXml());
                serializableSession.setFormXml(toBeSaved.getFormXml());
                serializableSession.setRestoreXml(toBeSaved.getRestoreXml());
                return null;
            }
        }).when(sessionRepo).save(any(SerializableSession.class));
        return sessionRepo;
    }

    @Bean
    public MenuRepo menuRepo() {
        MenuRepo menuRepo = Mockito.mock(MenuRepo.class);
        when(menuRepo.find(anyString())).thenReturn(serializableMenuSession);
        ArgumentCaptor<SerializableMenuSession> argumentCaptor = ArgumentCaptor.forClass(SerializableMenuSession.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableMenuSession toBeSaved = (SerializableMenuSession) args[0];
                serializableMenuSession.setActions(toBeSaved.getActions());
                return null;
            }
        }).when(menuRepo).save(any(SerializableMenuSession.class));
        return menuRepo;
    }

    @Bean
    public XFormService newFormRequest() {
        return Mockito.mock(XFormService.class);
    }

    @Bean
    public RestoreService restoreService() {
        RestoreService impl = Mockito.mock(RestoreService.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return FileUtils.getFile(this.getClass(), "test_restore.xml");
            }
        }).when(impl).getRestoreXml(anyString(), any(HqAuth.class));
        return impl;
    }
}