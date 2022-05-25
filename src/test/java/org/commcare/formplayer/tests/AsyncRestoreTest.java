package org.commcare.formplayer.tests;

import org.commcare.formplayer.exceptions.AsyncRetryException;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.utils.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AsyncRestoreTest {

    @Test
    public void testHandleAsyncResponse()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        boolean failure = false;
        RestoreFactory restoreFactory = new RestoreFactory();
        String asyncResponse = FileUtils.getFile(this.getClass(),
                "restores/async_restore_response.xml");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "30");
        Method method = restoreFactory.getClass().getDeclaredMethod("handleAsyncRestoreResponse",
                String.class, HttpHeaders.class);
        method.setAccessible(true);

        try {
            method.invoke(restoreFactory, asyncResponse, headers);
        } catch (InvocationTargetException invocationException) {
            AsyncRetryException e = (AsyncRetryException)invocationException.getTargetException();
            Assertions.assertEquals(143, e.getDone());
            Assertions.assertEquals(23311, e.getTotal());
            Assertions.assertEquals(30, e.getRetryAfter());
            Assertions.assertEquals("Asynchronous restore under way for large_caseload",
                    e.getMessage());
            failure = true;
        }
        Assertions.assertTrue(failure);
    }
}
