package org.commcare.formplayer.tests;

import org.commcare.formplayer.util.FormplayerSentry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.sentry.SentryClient;
import io.sentry.context.Context;
import io.sentry.event.Breadcrumb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FormplayerRavenTest {
    FormplayerSentry raven;
    Context contextMock;

    @BeforeEach
    public void setUp() throws Exception {
        SentryClient sentryMock = mock(SentryClient.class);
        contextMock = mock(Context.class);
        when(sentryMock.getContext()).thenReturn(contextMock);
        raven = new FormplayerSentry(sentryMock);
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
