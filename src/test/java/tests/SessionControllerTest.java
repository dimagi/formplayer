package tests;

import auth.HqAuth;
import objects.SerializableFormSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.ResultActions;
import utils.FileUtils;
import utils.TestContext;

import java.io.IOException;
import java.util.HashMap;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class SessionControllerTest extends BaseTestClass{


    @Before
    public void setUp() throws IOException {
        super.setUp();
        when(restoreServiceMock.getRestoreXml(anyString(), any(HqAuth.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), "test_restore.xml"));
    }

    @Test
    public void findSession() throws Exception {

        SerializableFormSession first = new SerializableFormSession();
        first.setId("1");
        first.setInstanceXml("<test1/>");

        when(sessionRepoMock.find("1")).thenReturn(first);

        ResultActions result = mockMvc.perform(get("/get_session/?id=1"));
        result.andExpect(status().isOk());

        //result.andExpect(model().attributeExists("id"));
        verify(sessionRepoMock, times(1)).find("1");
        verifyNoMoreInteractions(sessionRepoMock);
    }

    @Test
    public void findAllSessions() throws Exception {

        SerializableFormSession first = new SerializableFormSession();
        first.setId("1");
        first.setInstanceXml("<test1/>");
        SerializableFormSession second = new SerializableFormSession();
        second.setId("2");
        second.setInstanceXml("<test2/>");

        HashMap<Object, Object> sessions = new HashMap<Object, Object>();
        sessions.put("1", first);
        sessions.put("2", second);

        when(sessionRepoMock.findAll()).thenReturn(sessions);

        ResultActions result = mockMvc.perform(get("/sessions"));
        result.andExpect(status().isOk());

        //result.andExpect(model().attributeExists("id"));
        verify(sessionRepoMock, times(1)).findAll();
        verifyNoMoreInteractions(sessionRepoMock);
    }

    @Test
    public void newSession() throws Exception {
        startNewSession("requests/new_form/new_form_2.json", "xforms/basic.xml");
        verify(sessionRepoMock, times(1)).save(Mockito.any(SerializableFormSession.class));
        verifyNoMoreInteractions(sessionRepoMock);
    }
}
