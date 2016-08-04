package utils;

import auth.HqAuth;
import objects.SerializableFormSession;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import repo.FormSessionRepo;
import repo.MenuSessionRepo;
import repo.SerializableMenuSession;
import services.InstallService;
import services.RestoreService;
import services.SubmitService;
import services.XFormService;
import services.impl.InstallServiceImpl;
import services.impl.SubmitServiceImpl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@Configuration
public class TestContext {

    private static SerializableFormSession serializableFormSession;
    private static SerializableMenuSession serializableMenuSession;
 
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
        FormSessionRepo formSessionRepo = Mockito.mock(FormSessionRepo.class);
        when(formSessionRepo.findOne(anyString())).thenReturn(serializableFormSession);
        ArgumentCaptor<SerializableFormSession> argumentCaptor = ArgumentCaptor.forClass(SerializableFormSession.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableFormSession toBeSaved = (SerializableFormSession) args[0];
                serializableFormSession.setInstanceXml(toBeSaved.getInstanceXml());
                serializableFormSession.setFormXml(toBeSaved.getFormXml());
                serializableFormSession.setRestoreXml(toBeSaved.getRestoreXml());
                return null;
            }
        }).when(formSessionRepo).save(any(SerializableFormSession.class));
        return formSessionRepo;
    }


    @Bean
    public MenuSessionRepo menuSessionRepo() {
        MenuSessionRepo menuSessionRepo = Mockito.mock(MenuSessionRepo.class);
        when(menuSessionRepo.findOne(anyString())).thenReturn(serializableMenuSession);
        ArgumentCaptor<SerializableFormSession> argumentCaptor = ArgumentCaptor.forClass(SerializableFormSession.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                SerializableMenuSession toBeSaved = (SerializableMenuSession) args[0];
                serializableMenuSession.setCommcareSession(toBeSaved.getCommcareSession());
                return null;
            }
        }).when(menuSessionRepo).save(any(SerializableMenuSession.class));
        return menuSessionRepo;
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

    @Bean
    public InstallService installService(){
        return Mockito.mock(InstallServiceImpl.class);
    }

    @Bean
    public SubmitService submitService() {
        return Mockito.mock(SubmitServiceImpl.class);
    }
}