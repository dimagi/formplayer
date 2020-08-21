package org.commcare.formplayer.tests;

import org.commcare.formplayer.exceptions.AsyncRetryException;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.utils.FileUtils;
import org.commcare.formplayer.utils.TestContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by benrudolph on 10/13/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class AsyncRestoreTest {

    @Test
    public void testHandleAsyncResponse() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        boolean failure = false;
        RestoreFactory restoreFactory = new RestoreFactory();
        String asyncResponse = FileUtils.getFile(this.getClass(), "restores/async_restore_response.xml");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "30");
        Method method = restoreFactory.getClass().getDeclaredMethod("handleAsyncRestoreResponse", String.class, HttpHeaders.class);
        method.setAccessible(true);

        try {
            method.invoke(restoreFactory, asyncResponse, headers);
        } catch (InvocationTargetException invocationException) {
            AsyncRetryException e = (AsyncRetryException) invocationException.getTargetException();
            Assert.assertEquals(143, e.getDone());
            Assert.assertEquals(23311, e.getTotal());
            Assert.assertEquals(30, e.getRetryAfter());
            Assert.assertEquals("Asynchronous restore under way for large_caseload", e.getMessage());
            failure = true;
        }
        Assert.assertTrue(failure);
    }
}
