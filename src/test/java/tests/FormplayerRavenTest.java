package tests;

import com.getsentry.raven.Raven;
import com.getsentry.raven.context.Context;
import com.getsentry.raven.event.Breadcrumb;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import util.FormplayerRaven;
import utils.TestContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FormplayerRavenTest {
    FormplayerRaven raven;
    Context contextMock;

    @Before
    public void setUp() throws Exception {
        Raven ravenMock = mock(Raven.class);
        contextMock = mock(Context.class);
        when(ravenMock.getContext()).thenReturn(contextMock);
        raven = new FormplayerRaven(ravenMock);
    }

    @Test
    public void testBreadcrumbs() {
        raven.newBreadcrumb()
                .setData(
                        "a", "1",
                        "b", "2",
                        "c", "3"
                )
                .setCategory("Category X")
                .setLevel(Breadcrumb.Level.WARNING)
                .setType(Breadcrumb.Type.NAVIGATION)
                .setMessage("This is a test.")
                .record();

        ArgumentCaptor<Breadcrumb> captor = ArgumentCaptor.forClass(Breadcrumb.class);
        verify(contextMock).recordBreadcrumb(captor.capture());
        Breadcrumb breadcrumb = captor.getValue();
        assertEquals("1", breadcrumb.getData().get("a"));
        assertEquals("2", breadcrumb.getData().get("b"));
        assertEquals("3", breadcrumb.getData().get("c"));
        assertEquals("Category X", breadcrumb.getCategory());
        assertEquals(Breadcrumb.Level.WARNING, breadcrumb.getLevel());
        assertEquals(Breadcrumb.Type.NAVIGATION, breadcrumb.getType());
        assertEquals("This is a test.", breadcrumb.getMessage());
    }

    @Test
    public void testBreadcrumbsWithNullData() {
        ArgumentCaptor<Breadcrumb> captor = ArgumentCaptor.forClass(Breadcrumb.class);
        raven.newBreadcrumb()
                .setData("hello", null)
                .record();
        verify(contextMock).recordBreadcrumb(captor.capture());

        Breadcrumb breadcrumb = captor.getValue();
        assertEquals(null, breadcrumb.getData().get("hello"));
    }
}
