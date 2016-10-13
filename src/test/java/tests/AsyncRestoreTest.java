package tests;

import exceptions.AsyncRetryException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import services.impl.RestoreServiceImpl;
import utils.FileUtils;
import utils.TestContext;

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
        RestoreServiceImpl restoreService = new RestoreServiceImpl();
        String asyncResponse = FileUtils.getFile(this.getClass(), "restores/async_restore_response.xml");
        Method method = restoreService.getClass().getDeclaredMethod("handleAsyncRestoreResponse", String.class);
        method.setAccessible(true);

        try {
            method.invoke(restoreService, asyncResponse);
        } catch (InvocationTargetException invocationException) {
            AsyncRetryException e = (AsyncRetryException) invocationException.getTargetException();
            Assert.assertEquals(143, e.getDone());
            Assert.assertEquals(23311, e.getTotal());
            Assert.assertEquals(30, e.getRetryAfter());
            Assert.assertEquals("Asynchronous restore under way for large_caseload", e.getMessage());
            failure = false;
        }
        Assert.assertTrue(failure);
    }
}
